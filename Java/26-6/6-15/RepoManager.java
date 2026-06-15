/**
 * Git 仓库管理工具 (Java Swing 版)
 * 功能：选择仓库路径，管理远端地址、拉取、推送、提交
 */
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
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

    private final JTextField repoPathField = new JTextField();
    private final JLabel remoteInfoLabel = new JLabel("未选择仓库");
    private final JTextPane outputPane = new JTextPane();
    private final JLabel statusBar = new JLabel("就绪");
    private final StyledDocument outputDoc;
    private final Preferences prefs = Preferences.userNodeForPackage(GitRepoManager.class);

    public GitRepoManager() {
        setTitle("Git 仓库管理工具");
        setSize(720, 540);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        outputDoc = outputPane.getStyledDocument();
        setupUI();

        // 记忆上次打开的仓库
        String lastRepo = prefs.get(PREF_KEY_LAST_REPO, "");
        if (!lastRepo.isEmpty()) {
            repoPathField.setText(lastRepo);
            if (new File(lastRepo, ".git").isDirectory()) {
                SwingUtilities.invokeLater(this::loadRepo);
            } else {
                log("⚠ 上次的仓库路径已失效: " + lastRepo);
                prefs.remove(PREF_KEY_LAST_REPO);
            }
        }
    }

    // ── UI 搭建 ──────────────────────────────────────────────

    private void setupUI() {
        JPanel main = new JPanel(new BorderLayout(0, 8));
        main.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(main);

        // ── 路径选择区 ──
        JPanel pathPanel = new JPanel(new BorderLayout(8, 0));
        pathPanel.setBorder(BorderFactory.createTitledBorder("仓库路径"));

        JButton browseBtn = new JButton("浏览…");
        JButton loadBtn = new JButton("加载仓库");

        pathPanel.add(repoPathField, BorderLayout.CENTER);
        JPanel pathBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        pathBtns.add(loadBtn);
        pathBtns.add(browseBtn);
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

        centerPanel.add(actionPanel, BorderLayout.SOUTH);

        // ── 中间区域：上部分为信息/操作区，下部分为输出日志 ──
        JPanel middlePanel = new JPanel(new BorderLayout(0, 8));

        middlePanel.add(centerPanel, BorderLayout.NORTH);

        // ── 输出区 ──
        JPanel outPanel = new JPanel(new BorderLayout());
        outPanel.setBorder(BorderFactory.createTitledBorder("输出日志"));

        outputPane.setEditable(false);
        outputPane.setFont(new Font("Consolas", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(outputPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        outPanel.add(scrollPane, BorderLayout.CENTER);

        middlePanel.add(outPanel, BorderLayout.CENTER);

        main.add(middlePanel, BorderLayout.CENTER);

        // ── 底部状态栏 ──
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));
        main.add(statusBar, BorderLayout.SOUTH);

        // ── 事件绑定 ──
        browseBtn.addActionListener(e -> browseRepo());
        loadBtn.addActionListener(e -> loadRepo());
    }

    // ── 工具方法 ─────────────────────────────────────────────

    private void log(String text) {
        SwingUtilities.invokeLater(() -> {
            try {
                StyleContext sc = new StyleContext();
                Style style = sc.addStyle("default", null);
                if (text.startsWith("❌") || text.startsWith("⚠")) {
                    StyleConstants.setForeground(style, new Color(0xCC, 0x33, 0x33));
                } else if (text.startsWith("📂") || text.startsWith("🌐") || text.startsWith("ℹ")) {
                    StyleConstants.setForeground(style, new Color(0x22, 0x66, 0xCC));
                } else if (text.startsWith("$")) {
                    StyleConstants.setForeground(style, new Color(0x66, 0x66, 0x66));
                    StyleConstants.setBold(style, true);
                } else if (text.startsWith("──")) {
                    StyleConstants.setForeground(style, new Color(0x88, 0x88, 0x88));
                }
                outputDoc.insertString(outputDoc.getLength(), text + "\n", style);
                // 滚动到底部
                outputPane.setCaretPosition(outputDoc.getLength());
            } catch (BadLocationException ignored) {
            }
        });
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
                log("⚠ 退出码: " + exitCode);
            }
            setStatus(exitCode == 0 ? "完成" : "出错");
            return new GitResult(exitCode, stdout.toString(), stderr.toString());

        } catch (FileNotFoundException e) {
            log("❌ 未找到 git 命令，请确认已安装 Git");
            setStatus("错误");
            return new GitResult(-1, "", "git not found");
        } catch (IOException e) {
            log("❌ I/O 错误: " + e.getMessage());
            setStatus("错误");
            return new GitResult(-1, "", e.getMessage());
        } catch (InterruptedException e) {
            log("⏱ 命令执行被中断");
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

    private void browseRepo() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("选择 Git 仓库目录");

        // 默认打开上次的仓库目录
        String lastRepo = prefs.get(PREF_KEY_LAST_REPO, "");
        if (!lastRepo.isEmpty()) {
            chooser.setCurrentDirectory(new File(lastRepo));
        }

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            repoPathField.setText(path);
            if (new File(path, ".git").isDirectory()) {
                loadRepo();
            } else {
                remoteInfoLabel.setText("⚠ 所选目录不是 Git 仓库");
                log("⚠ 所选目录不是 Git 仓库");
            }
        }
    }

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
        log("📂 已加载仓库: " + path);
        listRemotes();
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
                logSb.append("🌐 远程仓库 (").append(seen.size()).append(" 个):");
                for (var entry : seen.entrySet()) {
                    logSb.append("\n  ").append(entry.getKey()).append("  →  ").append(entry.getValue());
                }
                log(logSb.toString());
            } else {
                remoteInfoLabel.setText("⚠ 没有配置远程仓库");
                log("⚠ 没有配置远程仓库");
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
        try {
            runGit("pull", "--all");
        } catch (Exception ignored) {
        }
        log("── 拉取结束 ──");
    }

    private void gitPush() {
        if (!checkRepo()) return;
        log("── 推送开始 ──");
        try {
            runGit("push", "--all");
            runGit("push", "--tags");
        } catch (Exception ignored) {
        }
        log("── 推送结束 ──");
    }

    private void gitCommit() {
        if (!checkRepo()) return;
        log("── 提交开始 ──");
        try {
            // 先检查是否有变更
            GitResult statusResult = runGit("status", "--porcelain");
            if (statusResult.exitCode != 0) return;
            if (statusResult.stdout.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "没有检测到需要提交的变更", "提示", JOptionPane.INFORMATION_MESSAGE);
                log("ℹ 没有变更需要提交");
                return;
            }

            // 询问提交信息
            String msg = JOptionPane.showInputDialog(this, "请输入提交信息:", "提交", JOptionPane.PLAIN_MESSAGE);
            if (msg == null || msg.trim().isEmpty()) {
                log("❌ 提交已取消");
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
        log("── 提交结束 ──");
    }

    private void gitStatus() {
        if (!checkRepo()) return;
        log("── 仓库状态 ──");
        try {
            runGit("status");
        } catch (Exception ignored) {
        }
        log("── 状态结束 ──");
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
}
