// Minecraft 服务器管理器
// 仅由 huidoudour(慧兜兜)学习使用
// 该程序仅用于学习和研究，不建议在生产环境中使用

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MinecraftServerManager extends JFrame {
    @Serial
    private static final long serialVersionUID = 1L;
    
    private transient Process serverProcess;
    private JTextArea consoleArea;
    private JTextField commandField;
    private JComboBox<String> memoryComboBox;
    private JButton startButton;
    private JButton stopButton;
    private JLabel statusLabel;
    private transient Thread outputThread;
    private transient Thread errorThread;
    private transient Thread monitorThread;

    private static final String SERVER_PATH = "mc";
    private static final String SERVER_JAR = "server.jar";
    private static final String JAVA_PATH = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

    private MinecraftServerManager() {
        super();
    }
    
    private void init() {
        initializeUI();
        updateServerStatus(false);
    }

    private void initializeUI() {
        // 先设置基本属性避免this逃逸
        setTitle("Minecraft 服务器管理器 v1.0");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 顶部控制面板
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // 内存选择
        controlPanel.add(new JLabel("内存设置:"));
        String[] memoryOptions = {"1024M", "2048M", "3072M", "4096M", "5120M", "6144M", "8192M"};
        memoryComboBox = new JComboBox<>(memoryOptions);
        memoryComboBox.setSelectedItem("2048M"); // 默认2048M
        controlPanel.add(memoryComboBox);

        // 启动/停止按钮
        startButton = new JButton("启动服务器");
        stopButton = new JButton("停止服务器");
        stopButton.setEnabled(false);
        
        startButton.addActionListener(_ -> startServer());
        stopButton.addActionListener(_ -> stopServer());
        
        controlPanel.add(startButton);
        controlPanel.add(stopButton);

        // 状态标签
        statusLabel = new JLabel("状态: 未运行");
        statusLabel.setForeground(Color.RED);
        controlPanel.add(statusLabel);

        add(controlPanel, BorderLayout.NORTH);

        // 控制台区域
        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.setBorder(BorderFactory.createTitledBorder("服务器控制台"));
        
        consoleArea = new JTextArea(20, 60);
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        consoleArea.setBackground(Color.BLACK);
        consoleArea.setForeground(Color.WHITE);
        
        // 自动滚动到底部
        DefaultCaret caret = (DefaultCaret) consoleArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        
        JScrollPane scrollPane = new JScrollPane(consoleArea);
        consolePanel.add(scrollPane, BorderLayout.CENTER);

        // 命令输入区域
        JPanel commandPanel = new JPanel(new BorderLayout());
        commandField = new JTextField();
        commandField.setEnabled(false);
        commandField.addActionListener(_ -> sendCommand());
        
        JButton sendButton = new JButton("发送");
        sendButton.setEnabled(false);
        sendButton.addActionListener(_ -> sendCommand());
        
        commandPanel.add(new JLabel("命令: "), BorderLayout.WEST);
        commandPanel.add(commandField, BorderLayout.CENTER);
        commandPanel.add(sendButton, BorderLayout.EAST);
        
        consolePanel.add(commandPanel, BorderLayout.SOUTH);
        
        add(consolePanel, BorderLayout.CENTER);

        // 设置窗口属性
        pack();
        setLocationRelativeTo(null);
        setResizable(true);
        
        // 添加窗口关闭监听器
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (serverProcess != null && serverProcess.isAlive()) {
                    stopServer();
                }
                System.exit(0);
            }
        });
    }

    private void startServer() {
        try {
            appendConsole("正在启动Minecraft服务器...");
            
            // 检查服务器文件是否存在
            File serverJar = new File(SERVER_PATH, SERVER_JAR);
            if (!serverJar.exists()) {
                JOptionPane.showMessageDialog(this, 
                    "服务器文件不存在: " + serverJar.getAbsolutePath(), 
                    "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 构建启动命令
            String memory = (String) memoryComboBox.getSelectedItem();
            List<String> command = buildServerCommand(memory);

            // 创建并配置进程构建器
            ProcessBuilder processBuilder = createProcessBuilder(command);
            
            // 启动服务器
            serverProcess = processBuilder.start();
            
            // 启动输出读取线程
            startOutputThreads();
            
            // 启动进程监控线程
            startMonitorThread();
            
            updateServerStatus(true);
            appendConsole("服务器启动成功！内存设置: " + memory);
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                "启动服务器失败: " + e.getMessage(), 
                "错误", JOptionPane.ERROR_MESSAGE);
            appendConsole("启动服务器失败: " + e.getMessage());
            updateServerStatus(false);
        }
    }

    private void stopServer() {
        try {
            if (serverProcess != null && serverProcess.isAlive()) {
                appendConsole("正在停止服务器...");
                
                // 发送停止命令
                try {
                    OutputStream outputStream = serverProcess.getOutputStream();
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream), true);
                    writer.println("stop");
                    writer.flush();
                    appendConsole("> stop");
                } catch (Exception e) {
                    appendConsole("发送停止命令失败: " + e.getMessage());
                }
                
                // 等待进程结束（最多30秒）
                boolean terminated = serverProcess.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
                
                if (!terminated) {
                    appendConsole("等待超时，正在强制停止服务器...");
                    serverProcess.destroyForcibly();
                    
                    // 再次等待强制停止
                    boolean forceTerminated = serverProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                    if (forceTerminated) {
                        appendConsole("服务器已强制停止");
                    } else {
                        appendConsole("警告：服务器进程可能仍在运行");
                    }
                } else {
                    appendConsole("服务器已正常停止");
                }
                
                // 停止输出线程
                stopOutputThreads();
                
                // 停止监控线程
                if (monitorThread != null && monitorThread.isAlive()) {
                    monitorThread.interrupt();
                }
                
                updateServerStatus(false);
            }
        } catch (InterruptedException e) {
            appendConsole("停止服务器时出错: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private void sendCommand() {
        String command = commandField.getText().trim();
        if (!command.isEmpty()) {
            sendCommandToServer(command);
            commandField.setText("");
        }
    }

    private void sendCommandToServer(String command) {
        if (serverProcess != null && serverProcess.isAlive()) {
            try {
                OutputStream outputStream = serverProcess.getOutputStream();
                // 使用UTF-8编码发送命令
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true);
                writer.println(command);
                writer.flush(); // 确保命令立即发送
                appendConsole("> " + command);
            } catch (Exception e) {
                appendConsole("发送命令失败: " + e.getMessage());
            }
        } else {
            appendConsole("服务器未运行，无法发送命令");
        }
    }

    /**
     * 构建服务器启动命令
     * @param memory 内存设置
     * @return 服务器启动命令列表
     */
    private List<String> buildServerCommand(String memory) {
        List<String> command = new ArrayList<>();
        command.add(JAVA_PATH);
        command.add("-Xmx" + memory);
        command.add("-Xms" + memory);
        command.add("-Dfile.encoding=UTF-8"); // 设置文件编码为UTF-8
        command.add("-Dsun.jnu.encoding=UTF-8"); // 设置JVM编码为UTF-8
        command.add("-jar");
        command.add(SERVER_JAR);
        command.add("nogui");
        return command;
    }

    /**
     * 创建并配置进程构建器
     * @param command 服务器启动命令
     * @return 配置好的进程构建器
     */
    private ProcessBuilder createProcessBuilder(List<String> command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(SERVER_PATH));
        
        // 设置环境变量以确保UTF-8编码
        Map<String, String> env = processBuilder.environment();
        env.put("JAVA_TOOL_OPTIONS", "-Dfile.encoding=UTF-8");
        env.put("LANG", "zh_CN.UTF-8");
        env.put("LC_ALL", "zh_CN.UTF-8");
        
        return processBuilder;
    }

    private void startOutputThreads() {
        // 标准输出线程
        outputThread = new Thread(() -> {
            // 使用UTF-8编码读取服务器输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(serverProcess.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while (!Thread.currentThread().isInterrupted() && (line = reader.readLine()) != null) {
                    appendConsole(line);
                }
            } catch (IOException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    appendConsole("读取服务器输出时出错: " + e.getMessage());
                }
            }
            appendConsole("服务器输出线程已停止");
        });
        outputThread.setDaemon(true);
        outputThread.start();

        // 错误输出线程
        errorThread = new Thread(() -> {
            // 使用UTF-8编码读取服务器错误输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(serverProcess.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while (!Thread.currentThread().isInterrupted() && (line = reader.readLine()) != null) {
                    appendConsole("[ERROR] " + line);
                }
            } catch (IOException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    appendConsole("读取服务器错误输出时出错: " + e.getMessage());
                }
            }
            appendConsole("服务器错误输出线程已停止");
        });
        errorThread.setDaemon(true);
        errorThread.start();
    }

    private void startMonitorThread() {
        monitorThread = new Thread(() -> {
            try {
                if (serverProcess != null) {
                    // 使用CompletableFuture异步处理进程结束事件
                    serverProcess.onExit().thenAccept(process -> {
                        appendConsole("检测到服务器进程已退出，退出码: " + process.exitValue());
                        SwingUtilities.invokeLater(() -> {
                            updateServerStatus(false);
                            stopOutputThreads();
                        });
                    }).exceptionally(ex -> {
                        appendConsole("监控进程时出错: " + ex.getMessage());
                        return null;
                    });
                    
                    // 等待进程结束，不占用CPU资源
                    int exitCode = serverProcess.waitFor();
                    appendConsole("服务器进程已结束，退出码: " + exitCode);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                appendConsole("监控线程被中断");
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.setName("ServerMonitor");
        monitorThread.start();
    }

    private void stopOutputThreads() {
        // 中断线程
        if (outputThread != null && outputThread.isAlive()) {
            outputThread.interrupt();
            try {
                outputThread.join(2000); // 等待最多2秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (errorThread != null && errorThread.isAlive()) {
            errorThread.interrupt();
            try {
                errorThread.join(2000); // 等待最多2秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void appendConsole(String message) {
        SwingUtilities.invokeLater(() -> {
            consoleArea.append("[" + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()) + "] " + message + "\n");
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }

    private void updateServerStatus(boolean isRunning) {
        SwingUtilities.invokeLater(() -> {
            if (isRunning) {
                statusLabel.setText("状态: 运行中");
                statusLabel.setForeground(Color.GREEN);
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                commandField.setEnabled(true);
                memoryComboBox.setEnabled(false);
            } else {
                statusLabel.setText("状态: 未运行");
                statusLabel.setForeground(Color.RED);
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
                commandField.setEnabled(false);
                memoryComboBox.setEnabled(true);
            }
        });
    }

    public static void main(String[] args) {
        // 设置系统外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // 日志记录外观设置失败，但不影响程序运行
            System.err.println("设置系统外观失败: " + e.getMessage());
        }

        // 在事件调度线程中创建和显示GUI
        SwingUtilities.invokeLater(() -> {
            MinecraftServerManager manager = new MinecraftServerManager();
            manager.init(); // 初始化UI
            manager.setVisible(true);
        });
    }
}