/**
 * Git 仓库管理工具 (Java Swing 版)
 * 功能：选择仓库路径，管理远端地址、拉取、推送、提交
 */
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.prefs.Preferences;

public class RepoManager {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new GitRepoManager().setVisible(true);
        });
    }
}

class GitRepoManager extends JFrame {
    private static final String PREF_KEY_LAST_REPO = "lastRepoPath";
    private static final String REPO_JSON_FILE = "repo.json";

    private final JTextField repoPathField = new JTextField();
    private final JLabel remoteInfoLabel = new JLabel("未选择仓库");
    private final JTextPane outputPane = new JTextPane();
    private final JLabel statusBar = new JLabel("就绪");
    private final StyledDocument outputDoc;
    private final Preferences prefs = Preferences.userNodeForPackage(GitRepoManager.class);

    // ── 多仓库管理 ──
    private final DefaultListModel<RepoPathInfo> repoListModel = new DefaultListModel<>();
    private final JList<RepoPathInfo> repoJList = new JList<>(repoListModel);
    private final java.util.List<String> repoPaths = new ArrayList<>();

    // ── 嵌入式 Shell 进程 ──
    private Process shellProcess;
    private BufferedWriter shellStdin;
    private Thread shellReaderThread;
    private volatile boolean shellRunning;
    private final java.util.List<String> cmdHistory = new ArrayList<>();
    private int cmdHistoryIndex = -1;
    private JTextField shellInputField;               // 命令输入行
    private final StringBuilder terminalBuffer = new StringBuilder(); // 输出缓冲区
    private javax.swing.Timer terminalFlushTimer;     // 批量刷新定时器
    private static final java.util.regex.Pattern ANSI_PATTERN =
            java.util.regex.Pattern.compile("\u001B(?:[@-Z\\-_]|\\[[0-?]*[ -/]*[@-~])");

    public GitRepoManager() {
        setTitle("Git 仓库管理工具");
        setSize(960, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        outputDoc = outputPane.getStyledDocument();

        // 终端输出缓冲定时器：每 50ms 批量 flush，减少 EDT 调用
        terminalFlushTimer = new javax.swing.Timer(50, e -> flushTerminalBuffer());
        terminalFlushTimer.setRepeats(false);

        setupUI();

        // 窗口关闭时清理 PowerShell 进程
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                stopShell();
            }
        });

        // 加载保存的仓库列表
        loadRepoList();

        // 记忆上次打开的仓库
        String lastRepo = prefs.get(PREF_KEY_LAST_REPO, "");
        if (!lastRepo.isEmpty()) {
            repoPathField.setText(lastRepo);
            if (new File(lastRepo, ".git").isDirectory()) {
                // 选中对应的仓库项
                selectRepoInList(lastRepo);
                SwingUtilities.invokeLater(this::loadRepo);
            } else {
                log("[警告] 上次的仓库路径已失效: " + lastRepo);
                prefs.remove(PREF_KEY_LAST_REPO);
            }
        }
    }

    // ── UI 搭建 ──────────────────────────────────────────────

    private void setupUI() {
        // ── 整体布局：左右分栏 ──
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setBorder(null);
        splitPane.setDividerSize(6);
        splitPane.setResizeWeight(0.2); // 左栏占 20%
        splitPane.setDividerLocation(220);

        // ── 左栏：仓库列表 ──
        splitPane.setLeftComponent(buildLeftPanel());

        // ── 右栏：原有操作界面 ──
        splitPane.setRightComponent(buildRightPanel());

        add(splitPane, BorderLayout.CENTER);

        // ── 底部状态栏 ──
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        add(statusBar, BorderLayout.SOUTH);
    }

    /** 构建左栏：仓库列表 */
    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 4));

        JLabel title = new JLabel("我的仓库");
        title.setFont(uiFont(Font.BOLD, 14));
        panel.add(title, BorderLayout.NORTH);

        // 仓库列表
        repoJList.setFont(uiFont(Font.PLAIN, 12));
        repoJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        repoJList.setFixedCellHeight(48);
        repoJList.setBorder(BorderFactory.createLineBorder(new Color(0xDD, 0xDD, 0xDD)));
        repoJList.setCellRenderer(new RepoListCellRenderer());

        // 双击或回车切换到选中的仓库
        repoJList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    switchToSelectedRepo();
                }
            }
        });
        repoJList.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    switchToSelectedRepo();
                }
            }
        });

        JScrollPane listScroll = new JScrollPane(repoJList);
        listScroll.setBorder(null);
        panel.add(listScroll, BorderLayout.CENTER);

        // 按钮行
        JPanel btnPanel = new JPanel(new GridLayout(0, 1, 0, 4));

        JButton addBtn = new JButton("添加仓库");
        addBtn.setFocusPainted(false);
        addBtn.addActionListener(e -> addRepoByBrowser());

        JButton removeBtn = new JButton("移除仓库");
        removeBtn.setFocusPainted(false);
        removeBtn.addActionListener(e -> removeSelectedRepo());

        JButton switchBtn = new JButton("切换仓库");
        switchBtn.setFocusPainted(false);
        switchBtn.addActionListener(e -> switchToSelectedRepo());

        btnPanel.add(addBtn);
        btnPanel.add(switchBtn);
        btnPanel.add(removeBtn);

        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    /** 构建右栏：原有操作界面 */
    private JPanel buildRightPanel() {
        JPanel main = new JPanel(new BorderLayout(0, 8));
        main.setBorder(BorderFactory.createEmptyBorder(10, 8, 10, 10));

        // ── 路径选择区 ──
        JPanel pathPanel = new JPanel(new BorderLayout(8, 0));
        pathPanel.setBorder(BorderFactory.createTitledBorder("仓库路径"));

        JButton loadBtn = new JButton("加载仓库");

        pathPanel.add(repoPathField, BorderLayout.CENTER);
        JPanel pathBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        pathBtns.add(loadBtn);
        pathPanel.add(pathBtns, BorderLayout.EAST);

        main.add(pathPanel, BorderLayout.NORTH);

        // ── 中间面板 ──
        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));

        // ── 远程信息 ──
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("远程信息"));
        remoteInfoLabel.setForeground(new Color(0x55, 0x55, 0x55));
        infoPanel.add(remoteInfoLabel, BorderLayout.WEST);
        centerPanel.add(infoPanel, BorderLayout.NORTH);

        // ── 远程管理 ──
        JPanel remoteMgrPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        remoteMgrPanel.setBorder(BorderFactory.createTitledBorder("远程地址管理"));

        remoteMgrPanel.add(new JButton(new RemoteAction("查看远程", this::listRemotes)));
        remoteMgrPanel.add(new JButton(new RemoteAction("添加远程", this::addRemote)));
        remoteMgrPanel.add(new JButton(new RemoteAction("修改远程", this::setRemoteUrl)));
        remoteMgrPanel.add(new JButton(new RemoteAction("删除远程", this::removeRemote)));

        centerPanel.add(remoteMgrPanel, BorderLayout.CENTER);

        // ── 仓库操作 ──
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        actionPanel.setBorder(BorderFactory.createTitledBorder("仓库操作"));

        JButton pullBtn = new JButton("拉取 (Pull)");
        pullBtn.addActionListener(e -> gitPull());
        actionPanel.add(pullBtn);

        JButton pushBtn = new JButton("推送 (Push)");
        pushBtn.addActionListener(e -> gitPush());
        actionPanel.add(pushBtn);

        JButton commitBtn = new JButton("提交 (Commit)");
        commitBtn.addActionListener(e -> gitCommit());
        actionPanel.add(commitBtn);

        JButton statusBtn = new JButton("当前状态");
        statusBtn.addActionListener(e -> gitStatus());
        actionPanel.add(statusBtn);

        // ── 退出按钮 ──
        JPanel actionBottom = new JPanel(new BorderLayout());
        JButton exitBtn = new JButton("退出");
        exitBtn.addActionListener(e -> System.exit(0));
        JPanel exitPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        exitPanel.add(exitBtn);
        actionBottom.add(actionPanel, BorderLayout.CENTER);
        actionBottom.add(exitPanel, BorderLayout.EAST);

        centerPanel.add(actionBottom, BorderLayout.SOUTH);

        // ── 中间区域：上部分为信息/操作区，下部分为输出日志 ──
        JPanel middlePanel = new JPanel(new BorderLayout(0, 8));

        middlePanel.add(centerPanel, BorderLayout.NORTH);

        // ── 终端区：只读输出 + 独立命令输入行 ──
        JPanel terminalPanel = new JPanel(new BorderLayout());
        terminalPanel.setBorder(BorderFactory.createTitledBorder("终端"));

        // 上方：只读输出区域
        outputPane.setEditable(false);
        outputPane.setFont(new Font("Monospaced", Font.PLAIN, 13));
        outputPane.setBackground(new Color(0x0C, 0x0C, 0x0C));
        outputPane.setForeground(new Color(0xCC, 0xCC, 0xCC));
        outputPane.setCaretColor(new Color(0xCC, 0xCC, 0xCC));
        JScrollPane logScroll = new JScrollPane(outputPane);
        logScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // 下方：命令输入行
        shellInputField = new JTextField();
        shellInputField.setFont(new Font("Monospaced", Font.PLAIN, 13));
        shellInputField.setBackground(new Color(0x0C, 0x0C, 0x0C));
        shellInputField.setForeground(new Color(0xCC, 0xCC, 0xCC));
        shellInputField.setCaretColor(new Color(0xCC, 0xCC, 0xCC));
        shellInputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x33, 0x33, 0x33)),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        shellInputField.setEnabled(false); // 未连接 shell 前禁用

        terminalPanel.add(logScroll, BorderLayout.CENTER);
        terminalPanel.add(shellInputField, BorderLayout.SOUTH);

        // 清空按钮
        JButton clearLogBtn = new JButton("清空");
        clearLogBtn.setFont(uiFont(Font.PLAIN, 11));
        clearLogBtn.setFocusPainted(false);
        clearLogBtn.addActionListener(e -> clearOutput());
        JPanel logTopBar = new JPanel(new BorderLayout());
        logTopBar.add(clearLogBtn, BorderLayout.EAST);
        logTopBar.setOpaque(false);
        terminalPanel.add(logTopBar, BorderLayout.NORTH);

        middlePanel.add(terminalPanel, BorderLayout.CENTER);

        main.add(middlePanel, BorderLayout.CENTER);

        // ── 事件绑定 ──
        loadBtn.addActionListener(e -> loadRepo());

        // ── 输入框键盘：Enter 发送命令，↑↓ 浏览历史，Ctrl+L 清屏 ──
        shellInputField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    e.consume();
                    String cmd = shellInputField.getText().trim();
                    if (!cmd.isEmpty()) {
                        sendToShell(cmd);
                    }
                } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_UP) {
                    e.consume();
                    navigateHistory(-1);
                } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN) {
                    e.consume();
                    navigateHistory(1);
                } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_L
                        && (e.getModifiersEx() & java.awt.event.InputEvent.CTRL_DOWN_MASK) != 0) {
                    e.consume();
                    clearOutput();
                }
            }
        });

        return main;
    }

    // ── 工具方法 ─────────────────────────────────────────────

    /** 跨平台 UI 字体：Windows 用微软雅黑，Linux/Mac 用 SansSerif */
    private static Font uiFont(int style, int size) {
        String name = System.getProperty("os.name").toLowerCase().contains("win") ? "微软雅黑" : "SansSerif";
        return new Font(name, style, size);
    }

    private void log(String text) {
        // 同步写入（始终在 EDT 调用，无需 invokeLater）
        try {
            StyleContext sc = new StyleContext();
            Style style = sc.addStyle("default", null);
            if (text.startsWith("[错误]") || text.startsWith("[警告]")) {
                StyleConstants.setForeground(style, new Color(0xCC, 0x33, 0x33));
            } else if (text.startsWith("[信息]")) {
                StyleConstants.setForeground(style, new Color(0x22, 0x66, 0xCC));
            } else if (text.startsWith("$")) {
                StyleConstants.setForeground(style, new Color(0x66, 0x66, 0x66));
                StyleConstants.setBold(style, true);
            } else if (text.startsWith("──")) {
                StyleConstants.setForeground(style, new Color(0x88, 0x88, 0x88));
            }
            outputDoc.insertString(outputDoc.getLength(), text + "\n", style);
            outputPane.setCaretPosition(outputDoc.getLength());
        } catch (BadLocationException ignored) {
        }
    }

    private void setStatus(String text) {
        SwingUtilities.invokeLater(() -> statusBar.setText(text));
    }

    private String getRepoPath() {
        return repoPathField.getText().trim();
    }

    private GitResult runGit(String... args) {
        String repoPath = getRepoPath();
        StringBuilder cmdStr = new StringBuilder("git");
        for (String arg : args) {
            cmdStr.append(" ").append(arg);
        }
        log("$ " + cmdStr);
        setStatus("执行中…");

        ProcessBuilder pb = new ProcessBuilder("git");
        for (String arg : args) {
            pb.command().add(arg);
        }
        pb.directory(new File(repoPath));
        pb.redirectErrorStream(false);

        try {
            Process process = pb.start();

            // 读取 stdout
            BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "utf-8"));
            StringBuilder stdout = new StringBuilder();
            String line;
            while ((line = stdoutReader.readLine()) != null) {
                stdout.append(line).append("\n");
                log(line);
            }

            // 读取 stderr
            BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "utf-8"));
            StringBuilder stderr = new StringBuilder();
            while ((line = stderrReader.readLine()) != null) {
                stderr.append(line).append("\n");
                log(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log("[警告] 退出码: " + exitCode);
            }
            setStatus(exitCode == 0 ? "完成" : "出错");
            return new GitResult(exitCode, stdout.toString(), stderr.toString());

        } catch (FileNotFoundException e) {
            log("[错误] 未找到 git 命令，请确认已安装 Git");
            setStatus("错误");
            return new GitResult(-1, "", "git not found");
        } catch (IOException e) {
            log("[错误] I/O 错误: " + e.getMessage());
            setStatus("错误");
            return new GitResult(-1, "", e.getMessage());
        } catch (InterruptedException e) {
            log("[超时] 命令执行被中断");
            setStatus("中断");
            Thread.currentThread().interrupt();
            return new GitResult(-1, "", e.getMessage());
        }
    }

    private boolean checkRepo() {
        String path = getRepoPath();
        if (path.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请先选择仓库路径", "提示", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        File gitDir = new File(path, ".git");
        if (!gitDir.isDirectory()) {
            JOptionPane.showMessageDialog(this, "所选目录不是一个 Git 仓库（没有 .git 目录）", "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    // ── 事件处理 ─────────────────────────────────────────────

    private void loadRepo() {
        String path = getRepoPath();
        if (path.isEmpty() || !new File(path).isDirectory()) {
            JOptionPane.showMessageDialog(this, "路径不存在", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!new File(path, ".git").isDirectory()) {
            JOptionPane.showMessageDialog(this, "不是有效的 Git 仓库", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // 保存路径以便下次启动时恢复
        prefs.put(PREF_KEY_LAST_REPO, path);
        log("[信息] 已加载仓库: " + path);

        // 自动加入左侧列表
        if (!repoPaths.contains(path)) {
            repoPaths.add(path);
            repoListModel.addElement(new RepoPathInfo(path));
            saveRepoList();
        }
        selectRepoInList(path);

        listRemotes();

        // 清空终端，启动纯 PowerShell（不混入任何日志）
        try {
            outputDoc.remove(0, outputDoc.getLength());
        } catch (BadLocationException ignored) {}
        startShell(path);
    }

    private void listRemotes() {
        if (!checkRepo()) return;
        try {
            GitResult result = runGit("remote", "-v");
            if (result.exitCode != 0) return;

            String output = result.stdout.trim();
            if (!output.isEmpty()) {
                String[] lines = output.split("\n");
                java.util.LinkedHashMap<String, String> seen = new java.util.LinkedHashMap<>();
                for (String line : lines) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 2) {
                        seen.put(parts[0], parts[1]);
                    }
                }
                StringBuilder sb = new StringBuilder();
                sb.append("<html>");
                for (var entry : seen.entrySet()) {
                    if (sb.length() > 6) sb.append("<br>");
                    sb.append("  ").append(entry.getKey()).append("  →  ").append(entry.getValue());
                }
                sb.append("</html>");
                remoteInfoLabel.setText(sb.toString());

                StringBuilder logSb = new StringBuilder();
                logSb.append("远程仓库 (").append(seen.size()).append(" 个):");
                for (var entry : seen.entrySet()) {
                    logSb.append("\n  ").append(entry.getKey()).append("  →  ").append(entry.getValue());
                }
                log(logSb.toString());
            } else {
                remoteInfoLabel.setText("没有配置远程仓库");
                log("[警告] 没有配置远程仓库");
            }
        } catch (Exception ignored) {
        }
    }

    private void addRemote() {
        if (!checkRepo()) return;
        String name = JOptionPane.showInputDialog(this, "远程名称 (如 origin):", "添加远程", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        String url = JOptionPane.showInputDialog(this, "远程 URL (" + name + "):", "添加远程", JOptionPane.PLAIN_MESSAGE);
        if (url == null || url.trim().isEmpty()) return;

        try {
            GitResult result = runGit("remote", "add", name.trim(), url.trim());
            if (result.exitCode == 0) {
                JOptionPane.showMessageDialog(this, "已添加远程仓库 " + name, "成功", JOptionPane.INFORMATION_MESSAGE);
                listRemotes();
            }
        } catch (Exception ignored) {
        }
    }

    private void setRemoteUrl() {
        if (!checkRepo()) return;
        String name = JOptionPane.showInputDialog(this, "要修改的远程名称 (如 origin):", "修改远程", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        String url = JOptionPane.showInputDialog(this, "新的 URL (" + name + "):", "修改远程", JOptionPane.PLAIN_MESSAGE);
        if (url == null || url.trim().isEmpty()) return;

        try {
            GitResult result = runGit("remote", "set-url", name.trim(), url.trim());
            if (result.exitCode == 0) {
                JOptionPane.showMessageDialog(this, "已修改远程 " + name + " 的 URL", "成功", JOptionPane.INFORMATION_MESSAGE);
                listRemotes();
            }
        } catch (Exception ignored) {
        }
    }

    private void removeRemote() {
        if (!checkRepo()) return;
        String name = JOptionPane.showInputDialog(this, "要删除的远程名称:", "删除远程", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        int confirm = JOptionPane.showConfirmDialog(this, "确定要删除远程仓库 " + name + " 吗？", "确认", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            GitResult result = runGit("remote", "remove", name.trim());
            if (result.exitCode == 0) {
                JOptionPane.showMessageDialog(this, "已删除远程 " + name, "成功", JOptionPane.INFORMATION_MESSAGE);
                listRemotes();
            }
        } catch (Exception ignored) {
        }
    }

    private void gitPull() {
        if (!checkRepo()) return;
        log("── 拉取开始 ──");
        try { runGit("pull", "--all"); } catch (Exception ignored) {}
        log("── 拉取结束 ──");
    }

    private void gitPush() {
        if (!checkRepo()) return;
        log("── 推送开始 ──");
        try {
            runGit("push", "--all");
            runGit("push", "--tags");
        } catch (Exception ignored) {}
        log("── 推送结束 ──");
    }

    private void gitCommit() {
        if (!checkRepo()) return;
        try {
            // 先检查是否有变更（保留同步检查）
            GitResult statusResult = runGit("status", "--porcelain");
            if (statusResult.exitCode != 0) return;
            if (statusResult.stdout.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "没有检测到需要提交的变更", "提示", JOptionPane.INFORMATION_MESSAGE);
                log("[信息] 没有变更需要提交");
                return;
            }

            // 询问提交信息
            String msg = JOptionPane.showInputDialog(this, "请输入提交信息:", "提交", JOptionPane.PLAIN_MESSAGE);
            if (msg == null || msg.trim().isEmpty()) {
                log("[错误] 提交已取消");
                return;
            }

            // add 所有变更
            runGit("add", "-A");
            // commit
            GitResult result = runGit("commit", "-m", msg.trim());
            if (result.exitCode == 0) {
                JOptionPane.showMessageDialog(this,
                        "提交成功！\n\n注：仅提交到本地，需点击「推送」才会上传到远程。",
                        "成功", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ignored) {
        }
    }

    private void gitStatus() {
        if (!checkRepo()) return;
        log("── 仓库状态 ──");
        try { runGit("status"); } catch (Exception ignored) {}
        log("── 状态结束 ──");
    }

    // ═══════════════════════════════════════════════════════════
    // ── 多仓库管理 ─────────────────────────────────────────────
    // ═══════════════════════════════════════════════════════════

    /** 当前目录下的 repo.json 文件路径 */
    private File repoJsonFile() {
        return new File(System.getProperty("user.dir"), REPO_JSON_FILE);
    }

    /** 加载 repo.json 中的仓库列表 */
    private void loadRepoList() {
        File file = repoJsonFile();
        if (!file.exists()) return;

        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            parseRepoJson(content);
        } catch (IOException e) {
            log("[警告] 读取 " + REPO_JSON_FILE + " 失败: " + e.getMessage());
        }
    }

    /** 解析 JSON 格式的仓库列表（简易实现，不依赖第三方库） */
    private void parseRepoJson(String json) {
        // 格式: {"repos":["path1","path2"]}
        int start = json.indexOf("\"repos\"");
        if (start < 0) return;

        int arrStart = json.indexOf('[', start);
        int arrEnd = json.indexOf(']', arrStart);
        if (arrStart < 0 || arrEnd < 0) return;

        String arrContent = json.substring(arrStart + 1, arrEnd).trim();
        if (arrContent.isEmpty()) return;

        int idx = 0;
        while (idx < arrContent.length()) {
            int q1 = arrContent.indexOf('"', idx);
            if (q1 < 0) break;
            int q2 = arrContent.indexOf('"', q1 + 1);
            if (q2 < 0) break;
            String path = arrContent.substring(q1 + 1, q2);
            // 转义还原
            path = path.replace("\\/", "/").replace("\\\\", "\\");
            if (!path.isEmpty()) {
                repoPaths.add(path);
                repoListModel.addElement(new RepoPathInfo(path));
            }
            idx = q2 + 1;
        }
    }

    /** 保存仓库列表到 repo.json */
    private void saveRepoList() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"repos\": [\n");
        for (int i = 0; i < repoPaths.size(); i++) {
            String escaped = repoPaths.get(i)
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"");
            sb.append("    \"").append(escaped).append("\"");
            if (i < repoPaths.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n}\n");

        try {
            Files.write(repoJsonFile().toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log("[警告] 写入 " + REPO_JSON_FILE + " 失败: " + e.getMessage());
        }
    }

    /** 通过浏览选择路径，添加到仓库列表 */
    private void addRepoByBrowser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("选择 Git 仓库目录添加到列表");
        chooser.setApproveButtonText("添加到列表");

        // 默认打开上次的仓库目录
        String lastRepo = prefs.get(PREF_KEY_LAST_REPO, "");
        if (!lastRepo.isEmpty()) {
            chooser.setCurrentDirectory(new File(lastRepo));
        }

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        String path = chooser.getSelectedFile().getAbsolutePath();

        // 检查是否为 Git 仓库
        if (!new File(path, ".git").isDirectory()) {
            int ret = JOptionPane.showConfirmDialog(this,
                    "所选目录不是 Git 仓库（没有 .git 目录），\n仍然添加到列表吗？",
                    "提示", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ret != JOptionPane.YES_OPTION) return;
        }

        if (repoPaths.contains(path)) {
            JOptionPane.showMessageDialog(this, "该仓库已在列表中", "提示", JOptionPane.INFORMATION_MESSAGE);
            selectRepoInList(path);
            return;
        }

        repoPaths.add(path);
        repoListModel.addElement(new RepoPathInfo(path));
        saveRepoList();
        selectRepoInList(path);
        log("[信息] 已添加仓库到列表: " + path);
    }

    /** 从列表中删除选中的仓库 */
    private void removeSelectedRepo() {
        int idx = repoJList.getSelectedIndex();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "请先在左侧列表中选择要删除的仓库", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "确定要从列表中移除仓库「" + repoListModel.get(idx) + "」吗？\n（不会删除实际目录）",
                "确认删除", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        repoPaths.remove(idx);
        repoListModel.remove(idx);
        saveRepoList();
        log("[信息] 已从列表中移除仓库");
    }

    /** 切换到列表中选中的仓库 */
    private void switchToSelectedRepo() {
        int idx = repoJList.getSelectedIndex();
        if (idx < 0) {
            JOptionPane.showMessageDialog(this, "请先在左侧列表中选择一个仓库", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String path = repoPaths.get(idx);
        repoPathField.setText(path);

        if (!new File(path, ".git").isDirectory()) {
            JOptionPane.showMessageDialog(this, "该仓库路径已失效（找不到 .git 目录）", "错误", JOptionPane.ERROR_MESSAGE);
            log("[警告] 仓库路径已失效: " + path);
            return;
        }

        clearOutput();
        loadRepo();
    }

    /** 清除输出日志 */
    private void clearOutput() {
        try {
            outputDoc.remove(0, outputDoc.getLength());
        } catch (BadLocationException ignored) {
        }
    }

    // ── 嵌入式 Shell ──────────────────────────────────

    /** 获取当前系统的 shell 命令 */
    private String[] getShellCommand() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new String[]{"powershell.exe", "-NoLogo", "-NoExit", "-Command", "-"};
        } else {
            return new String[]{"/bin/bash", "--norc"};
        }
    }

    /** 剥离 ANSI 转义码 */
    private static String stripAnsi(String text) {
        return ANSI_PATTERN.matcher(text).replaceAll("");
    }

    /** 启动嵌入式 Shell 进程 */
    private void startShell(String workingDir) {
        stopShell();
        try {
            ProcessBuilder pb = new ProcessBuilder(getShellCommand());
            if (workingDir != null && !workingDir.isEmpty()) {
                File dir = new File(workingDir);
                if (dir.isDirectory()) pb.directory(dir);
            }
            pb.redirectErrorStream(true);

            shellProcess = pb.start();
            shellStdin = new BufferedWriter(new OutputStreamWriter(
                    shellProcess.getOutputStream(), StandardCharsets.UTF_8));
            shellRunning = true;

            // 输出读取线程
            shellReaderThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(shellProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    char[] buf = new char[4096];
                    int len;
                    while (shellRunning && (len = reader.read(buf)) != -1) {
                        String text = new String(buf, 0, len);
                        appendTerminalRaw(text);
                    }
                } catch (IOException ignored) {
                } finally {
                    shellRunning = false;
                }
            }, "shell-reader");
            shellReaderThread.setDaemon(true);
            shellReaderThread.start();

            SwingUtilities.invokeLater(() -> shellInputField.setEnabled(true));
            setStatus("终端已就绪");

        } catch (IOException e) {
            log("[错误] 启动 Shell 失败: " + e.getMessage());
        }
    }

    /** 停止 Shell 进程 */
    private void stopShell() {
        shellRunning = false;
        if (shellStdin != null) {
            try { shellStdin.close(); } catch (IOException ignored) {}
            shellStdin = null;
        }
        if (shellProcess != null) {
            shellProcess.destroyForcibly();
            try { shellProcess.waitFor(2, java.util.concurrent.TimeUnit.SECONDS); } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            shellProcess = null;
        }
        if (shellReaderThread != null) {
            shellReaderThread.interrupt();
            shellReaderThread = null;
        }
        if (terminalFlushTimer != null) {
            terminalFlushTimer.stop();
        }
        SwingUtilities.invokeLater(() -> shellInputField.setEnabled(false));
    }

    /** 发送命令到 Shell */
    private void sendToShell(String cmd) {
        if (cmd.isEmpty()) return;

        // 加入历史
        if (cmdHistory.isEmpty() || !cmdHistory.get(cmdHistory.size() - 1).equals(cmd)) {
            cmdHistory.add(cmd);
        }
        cmdHistoryIndex = -1;

        // 清空输入框
        shellInputField.setText("");

        // shell 已死则异步重启，不阻塞 EDT（命令静默丢弃，用户在新 shell 中重输）
        if (shellProcess == null || !shellProcess.isAlive()) {
            new Thread(() -> startShell(getRepoPath()), "shell-restart").start();
        }

        // 发送命令
        if (shellStdin != null) {
            try {
                shellStdin.write(cmd);
                shellStdin.newLine();
                shellStdin.flush();
            } catch (IOException e) {
                log("[错误] 发送命令失败: " + e.getMessage());
                new Thread(() -> startShell(getRepoPath()), "shell-restart").start();
            }
        }
    }

    /** 追加 Shell 输出到缓冲区 */
    private void appendTerminalRaw(String text) {
        synchronized (terminalBuffer) {
            terminalBuffer.append(stripAnsi(text));
            if (terminalFlushTimer != null) {
                terminalFlushTimer.restart();
            }
        }
    }

    /** 将缓冲区内容刷新到输出面板 */
    private void flushTerminalBuffer() {
        String batch;
        synchronized (terminalBuffer) {
            if (terminalBuffer.length() == 0) return;
            batch = terminalBuffer.toString();
            terminalBuffer.setLength(0);
        }
        SwingUtilities.invokeLater(() -> {
            try {
                // 限制最大行数，防止内存溢出
                int maxLines = 5000;
                String full = outputDoc.getText(0, outputDoc.getLength());
                int lineCount = 0;
                for (int i = 0; i < full.length(); i++) {
                    if (full.charAt(i) == '\n') lineCount++;
                }
                if (lineCount > maxLines) {
                    // 截断前半部分
                    int cutPos = 0;
                    int toCut = lineCount - maxLines;
                    for (int i = 0, n = 0; i < full.length() && n < toCut; i++) {
                        if (full.charAt(i) == '\n') n++;
                        cutPos = i + 1;
                    }
                    outputDoc.remove(0, cutPos);
                }
                outputDoc.insertString(outputDoc.getLength(), batch, null);
                outputPane.setCaretPosition(outputDoc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    /** 上下键浏览命令历史 */
    private void navigateHistory(int direction) {
        if (cmdHistory.isEmpty()) return;
        int newIndex = cmdHistoryIndex;
        if (direction < 0) {
            // ↑：初次按跳到最新，之后向上滚动
            if (newIndex == -1) newIndex = cmdHistory.size() - 1;
            else if (newIndex > 0) newIndex--;
        } else {
            // ↓：向下滚动，-1 表示回到空
            if (newIndex == -1) return;
            else if (newIndex < cmdHistory.size() - 1) newIndex++;
            else newIndex = -1;
        }
        cmdHistoryIndex = newIndex;
        shellInputField.setText(newIndex == -1 ? "" : cmdHistory.get(newIndex));
    }

    /** 在列表中通过路径选中对应项 */
    private void selectRepoInList(String path) {
        for (int i = 0; i < repoPaths.size(); i++) {
            if (repoPaths.get(i).equals(path)) {
                repoJList.setSelectedIndex(i);
                repoJList.ensureIndexIsVisible(i);
                break;
            }
        }
    }

    // ── 辅助类 ───────────────────────────────────────────────

    private record GitResult(int exitCode, String stdout, String stderr) {
    }

    /**
     * 用于远程管理按钮的 Action，阻止按钮获得焦点（保持视觉整洁）
     */
    private static class RemoteAction extends AbstractAction {
        private final Runnable action;

        RemoteAction(String name, Runnable action) {
            super(name);
            this.action = action;
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            action.run();
        }
    }

    // ── 仓库列表数据与渲染 ───────────────────────────────────

    /** 仓库列表项：包装路径，提供显示名称 */
    private static class RepoPathInfo {
        final String fullPath;
        final String displayName;

        RepoPathInfo(String fullPath) {
            this.fullPath = fullPath;
            this.displayName = getFolderName(fullPath);
        }

        /** 提取最后一级目录名作为显示名称 */
        private static String getFolderName(String path) {
            String normalized = path.replace("\\", "/");
            int idx = normalized.lastIndexOf('/');
            return idx >= 0 ? normalized.substring(idx + 1) : path;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /** 仓库列表的自定义单元格渲染器：两行显示（仓库名称 + 绝对路径） */
    private static class RepoListCellRenderer extends JPanel implements ListCellRenderer<RepoPathInfo> {
        private final JLabel nameLabel = new JLabel();
        private final JLabel pathLabel = new JLabel();

        RepoListCellRenderer() {
            setLayout(new BorderLayout(0, 0));
            setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));

            nameLabel.setFont(uiFont(Font.BOLD, 12));
            pathLabel.setFont(new Font("Monospaced", Font.PLAIN, 10));
            pathLabel.setForeground(new Color(0x88, 0x88, 0x88));

            JPanel textPanel = new JPanel(new BorderLayout(0, 1));
            textPanel.setOpaque(false);
            textPanel.add(nameLabel, BorderLayout.NORTH);
            textPanel.add(pathLabel, BorderLayout.SOUTH);
            add(textPanel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends RepoPathInfo> list,
                                                       RepoPathInfo value, int index,
                                                       boolean isSelected, boolean cellHasFocus) {
            if (value == null) return this;

            nameLabel.setText(value.displayName);
            pathLabel.setText(value.fullPath);

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                nameLabel.setForeground(list.getSelectionForeground());
                pathLabel.setForeground(list.getSelectionForeground().brighter());
            } else {
                setBackground(list.getBackground());
                nameLabel.setForeground(list.getForeground());
                pathLabel.setForeground(new Color(0x88, 0x88, 0x88));
            }
            setOpaque(true);
            return this;
        }
    }
}
