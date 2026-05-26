// Android应用管理工具
// 仅由 huidoudour(慧兜兜)学习使用
// 该程序仅用于学习和研究，不建议在生产环境中使用
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Android应用管理工具
 * 通过ADB管理Android设备和AVD上的应用
 * 采用双栏布局展示多用户应用（类似Android桌面启动器抽屉模式）
 * 
 * @author huidoudour
 * @version 3.0 - UI美化版
 */
public class AndroidAppManager extends JFrame {
    
    // 配色方案 - 柔和护眼版
    private static final Color PRIMARY_COLOR = new Color(70, 130, 180);        // 钢蓝色（柔和）
    private static final Color SUCCESS_COLOR = new Color(60, 140, 90);         // 柔和绿
    private static final Color WARNING_COLOR = new Color(210, 160, 50);        // 柔和黄
    private static final Color DANGER_COLOR = new Color(190, 70, 70);          // 柔和红
    private static final Color BACKGROUND_COLOR = new Color(245, 246, 247);    // 浅灰背景
    private static final Color CARD_COLOR = new Color(252, 252, 253);          // 卡片白
    private static final Color TEXT_PRIMARY = new Color(50, 50, 50);           // 主文字
    private static final Color TEXT_SECONDARY = new Color(100, 100, 100);      // 次要文字
    private static final Color BORDER_COLOR = new Color(220, 222, 225);        // 边框色
    private static final Color HOVER_COLOR = new Color(235, 240, 245);         // 悬停色
    
    // UI组件
    private JComboBox<String> deviceComboBox;
    private JTable user0Table;      // 用户0（主用户）应用表 - 左栏
    private JTable otherUserTable;  // 其他用户应用表 - 右栏
    private DefaultTableModel user0Model;
    private DefaultTableModel otherUserModel;
    private JButton refreshButton;
    private JLabel statusLabel;
    
    // 数据存储
    private List<AppInfo> allApps;
    private Map<Integer, List<AppInfo>> userAppsMap; // 用户ID -> 应用列表
    
    /**
     * 应用信息类
     */
    static class AppInfo {
        String packageName;
        int userId;
        boolean isHuidoudour;
        
        public AppInfo(String packageName, int userId) {
            this.packageName = packageName;
            this.userId = userId;
            this.isHuidoudour = packageName.contains("huidoudour");
        }
        
        @Override
        public String toString() {
            return packageName + (isHuidoudour ? " ⭐" : "");
        }
    }
    
    public AndroidAppManager() {
        initializeUI();
        loadDevices();
    }
    
    /**
     * 初始化UI界面
     */
    private void initializeUI() {
        setTitle("Android应用管理工具 - 慧兜兜专用版");
        
        // 获取系统DPI缩放比例
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        AffineTransform transform = gc.getDefaultTransform();
        double dpiScaleX = transform.getScaleX();
        double dpiScaleY = transform.getScaleY();
        
        // 计算考虑DPI缩放后的窗口尺寸
        // 目标：在100% DPI下显示为1366x768
        int targetWidth = 1366;
        int targetHeight = 768;
        
        // 如果DPI不是100%，需要调整设置值以补偿缩放
        int windowWidth = (int)(targetWidth / dpiScaleX);
        int windowHeight = (int)(targetHeight / dpiScaleY);
        
        logInfo("系统DPI缩放: " + (dpiScaleX * 100) + "%");
        logInfo("设置窗口尺寸: " + windowWidth + "x" + windowHeight + " (期望显示: " + targetWidth + "x" + targetHeight + ")");
        
        // 设置默认窗口大小为1366x768（包括标题栏和边框的整体窗口）
        setSize(windowWidth, windowHeight);
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // 设置窗口图标（如果有的话）
        try {
            setIconImage(createPlaceholderIcon());
        } catch (Exception e) {
            // 忽略图标设置错误
        }
        
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // 顶部控制面板
        JPanel topPanel = createTopPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);
        
        // 中间双栏面板（左右分栏）
        JPanel centerPanel = createDualColumnPanel();
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        // 底部状态栏
        statusLabel = new JLabel("就绪 | 提示：右键点击应用可进行操作");
        statusLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        statusLabel.setForeground(TEXT_SECONDARY);
        statusLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(232, 234, 237)),
            BorderFactory.createEmptyBorder(10, 5, 10, 5)
        ));
        mainPanel.add(statusLabel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // 初始化数据
        allApps = new ArrayList<>();
        userAppsMap = new HashMap<>();
    }
    
    /**
     * 创建占位图标
     */
    private Image createPlaceholderIcon() {
        int size = 32;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 绘制圆形背景
        g2d.setColor(PRIMARY_COLOR);
        g2d.fillOval(2, 2, size - 4, size - 4);
        
        // 绘制Android机器人简图
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(10, 8, 12, 10);  // 头部
        g2d.drawRect(8, 18, 16, 10);  // 身体
        
        g2d.dispose();
        return image;
    }
    
    /**
     * 日志输出到控制台
     */
    private void log(String message) {
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        System.out.println("[" + timestamp + "] " + message);
    }
    
    /**
     * 日志输出（带级别）
     */
    private void logInfo(String message) {
        log("INFO: " + message);
    }
    
    private void logSuccess(String message) {
        log("SUCCESS: " + message);
    }
    
    private void logWarning(String message) {
        log("WARNING: " + message);
    }
    
    private void logError(String message) {
        log("ERROR: " + message);
    }
    
    /**
     * 创建顶部控制面板
     */
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        panel.setBackground(CARD_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(232, 234, 237), 1),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        
        // 设备选择标签
        JLabel deviceLabel = new JLabel("选择设备:");
        deviceLabel.setFont(new Font("微软雅黑", Font.BOLD, 13));
        deviceLabel.setForeground(TEXT_PRIMARY);
        panel.add(deviceLabel);
        
        // 设备下拉框
        deviceComboBox = new JComboBox<>();
        deviceComboBox.setPreferredSize(new Dimension(300, 32));
        deviceComboBox.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        deviceComboBox.setBackground(Color.WHITE);
        panel.add(deviceComboBox);
        
        // 刷新按钮
        refreshButton = createStyledButton("刷新设备", PRIMARY_COLOR);
        refreshButton.addActionListener(e -> loadDevices());
        panel.add(refreshButton);
        
        // 加载应用按钮
        JButton loadAppsButton = createStyledButton("加载应用", SUCCESS_COLOR);
        loadAppsButton.addActionListener(e -> loadApps());
        panel.add(loadAppsButton);
        
        return panel;
    }
    
    /**
     * 创建美化按钮（圆角设计）
     */
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // 绘制圆角背景
                g2d.setColor(getBackground());
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                
                // 绘制文字
                super.paintComponent(g);
                g2d.dispose();
            }
            
            @Override
            protected void paintBorder(Graphics g) {
                // 不绘制默认边框
            }
        };
        
        button.setFont(new Font("微软雅黑", Font.BOLD, 12));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(130, 36));
        
        // 鼠标悬停效果
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.brighter());
                button.repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
                button.repaint();
            }
        });
        
        return button;
    }
    
    /**
     * 创建双栏面板（左右分栏展示不同用户的应用）
     */
    private JPanel createDualColumnPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        
        // 左栏：用户0（主用户）
        JPanel leftPanel = createUserPanel("用户 0 (主用户)", true);
        panel.add(leftPanel);
        
        // 右栏：其他用户
        JPanel rightPanel = createUserPanel("其他用户", false);
        panel.add(rightPanel);
        
        return panel;
    }
    
    /**
     * 创建用户应用面板（圆角卡片设计）
     */
    private JPanel createUserPanel(String title, boolean isUser0) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(CARD_COLOR);
        
        // 圆角边框
        panel.setBorder(BorderFactory.createCompoundBorder(
            new RoundBorder(BORDER_COLOR, 12),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        
        // 标题面板
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBackground(CARD_COLOR);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        titleLabel.setForeground(isUser0 ? PRIMARY_COLOR : new Color(130, 80, 160)); // 更柔和的紫色
        titlePanel.add(titleLabel);
        panel.add(titlePanel, BorderLayout.NORTH);
        
        // 表格模型
        String[] columns = {"包名", "类型"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        if (isUser0) {
            user0Model = model;
        } else {
            otherUserModel = model;
        }
        
        JTable table = new JTable(model);
        if (isUser0) {
            user0Table = table;
        } else {
            otherUserTable = table;
        }
        
        // 美化表格
        setupStyledTable(table, isUser0);
        
        // 添加右键菜单
        addContextMenu(table, isUser0);
        
        // 添加双击事件
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showAppOperations(table, isUser0);
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(232, 234, 237)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 底部提示
        JLabel hintLabel = new JLabel("右键或双击应用进行操作");
        hintLabel.setHorizontalAlignment(SwingConstants.CENTER);
        hintLabel.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        hintLabel.setForeground(TEXT_SECONDARY);
        hintLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        panel.add(hintLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * 设置表格样式
     */
    private void setupStyledTable(JTable table, boolean isUser0) {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(28);
        table.getTableHeader().setReorderingAllowed(false);
        table.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        table.setGridColor(new Color(232, 234, 237));
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(0, 0));
        
        // 表头样式
        table.getTableHeader().setFont(new Font("微软雅黑", Font.BOLD, 12));
        table.getTableHeader().setBackground(isUser0 ? 
            new Color(227, 242, 253) : new Color(243, 229, 245));
        table.getTableHeader().setForeground(TEXT_PRIMARY);
        table.getTableHeader().setPreferredSize(new Dimension(0, 35));
        
        // 选中行样式
        table.setSelectionBackground(new Color(232, 240, 254));
        table.setSelectionForeground(TEXT_PRIMARY);
        
        // 自定义单元格渲染器
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, 
                    isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    // 交替行颜色
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(250, 250, 250));
                    
                    // huidoudour应用特殊标记
                    if (column == 1 && value != null && value.toString().contains("⭐")) {
                        setForeground(new Color(255, 152, 0)); // 橙色
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setForeground(TEXT_PRIMARY);
                        setFont(getFont().deriveFont(Font.PLAIN));
                    }
                }
                
                return c;
            }
        });
    }
    
    /**
     * 添加右键上下文菜单
     */
    private void addContextMenu(JTable table, boolean isUser0) {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        
        // 启动应用
        JMenuItem launchItem = createMenuItem("启动应用", "启动此应用");
        launchItem.addActionListener(e -> {
            AppInfo app = getSelectedApp(table, isUser0);
            if (app != null) {
                launchApp(app);
            }
        });
        popupMenu.add(launchItem);
        
        popupMenu.addSeparator();
        
        // 强制停止
        JMenuItem forceStopItem = createMenuItem("强制停止", "强制停止应用运行");
        forceStopItem.addActionListener(e -> {
            AppInfo app = getSelectedApp(table, isUser0);
            if (app != null) {
                forceStopApp(app);
            }
        });
        popupMenu.add(forceStopItem);
        
        // 杀死进程
        JMenuItem killProcessItem = createMenuItem("杀死进程", "杀死应用的所有进程");
        killProcessItem.addActionListener(e -> {
            AppInfo app = getSelectedApp(table, isUser0);
            if (app != null) {
                killAppProcess(app);
            }
        });
        popupMenu.add(killProcessItem);
        
        popupMenu.addSeparator();
        
        // 卸载应用
        JMenuItem uninstallItem = createMenuItem("卸载应用", "完全卸载此应用");
        uninstallItem.addActionListener(e -> {
            AppInfo app = getSelectedApp(table, isUser0);
            if (app != null) {
                uninstallApp(app);
            }
        });
        popupMenu.add(uninstallItem);
        
        // 清除数据
        JMenuItem clearDataItem = createMenuItem("清除数据", "清除所有应用数据（包括登录信息）");
        clearDataItem.addActionListener(e -> {
            AppInfo app = getSelectedApp(table, isUser0);
            if (app != null) {
                clearData(app);
            }
        });
        popupMenu.add(clearDataItem);
        
        // 清除缓存
        JMenuItem clearCacheItem = createMenuItem("清除缓存", "清除应用缓存文件");
        clearCacheItem.addActionListener(e -> {
            AppInfo app = getSelectedApp(table, isUser0);
            if (app != null) {
                clearCache(app);
            }
        });
        popupMenu.add(clearCacheItem);
        
        popupMenu.addSeparator();
        
        // 复制包名
        JMenuItem copyPackageItem = createMenuItem("复制包名", "复制包名到剪贴板");
        copyPackageItem.addActionListener(e -> {
            AppInfo app = getSelectedApp(table, isUser0);
            if (app != null) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new java.awt.datatransfer.StringSelection(app.packageName), null);
                String msg = "已复制包名: " + app.packageName;
                logInfo(msg);
                setStatus(msg);
            }
        });
        popupMenu.add(copyPackageItem);
        
        table.setComponentPopupMenu(popupMenu);
    }
    
    /**
     * 创建菜单项
     */
    private JMenuItem createMenuItem(String text, String tooltip) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        item.setToolTipText(tooltip);
        return item;
    }
    
    /**
     * 显示应用操作对话框
     */
    private void showAppOperations(JTable table, boolean isUser0) {
        AppInfo app = getSelectedApp(table, isUser0);
        if (app == null) {
            return;
        }
        
        // 创建美化面板
        JPanel panel = new JPanel(new GridLayout(6, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JButton launchBtn = createStyledButton("启动应用", SUCCESS_COLOR);
        launchBtn.setPreferredSize(new Dimension(200, 40));
        launchBtn.addActionListener(e -> {
            launchApp(app);
        });
        
        JButton forceStopBtn = createStyledButton("强制停止", WARNING_COLOR);
        forceStopBtn.setPreferredSize(new Dimension(200, 40));
        forceStopBtn.addActionListener(e -> {
            forceStopApp(app);
        });
        
        JButton killBtn = createStyledButton("杀死进程", DANGER_COLOR);
        killBtn.setPreferredSize(new Dimension(200, 40));
        killBtn.addActionListener(e -> {
            killAppProcess(app);
        });
        
        JButton uninstallBtn = createStyledButton("卸载应用", DANGER_COLOR);
        uninstallBtn.setPreferredSize(new Dimension(200, 40));
        uninstallBtn.addActionListener(e -> {
            uninstallApp(app);
        });
        
        JButton clearDataBtn = createStyledButton("清除数据", WARNING_COLOR);
        clearDataBtn.setPreferredSize(new Dimension(200, 40));
        clearDataBtn.addActionListener(e -> {
            clearData(app);
        });
        
        JButton clearCacheBtn = createStyledButton("清除缓存", PRIMARY_COLOR);
        clearCacheBtn.setPreferredSize(new Dimension(200, 40));
        clearCacheBtn.addActionListener(e -> {
            clearCache(app);
        });
        
        panel.add(launchBtn);
        panel.add(forceStopBtn);
        panel.add(killBtn);
        panel.add(uninstallBtn);
        panel.add(clearDataBtn);
        panel.add(clearCacheBtn);
        
        // 使用 PLAIN_MESSAGE 移除默认的问号图标
        JOptionPane.showMessageDialog(
            this,
            panel,
            "选择对 \"" + app.packageName + "\" 的操作",
            JOptionPane.PLAIN_MESSAGE
        );
    }
    
    /**
     * 加载已连接的设备
     */
    private void loadDevices() {
        logInfo("开始检测设备...");
        setStatus("正在检测设备...");
        deviceComboBox.removeAllItems();
        
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"adb", "devices"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            String line;
            boolean firstLine = true;
            int deviceCount = 0;
            while ((line = reader.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue; // 跳过标题行
                }
                
                if (line.trim().isEmpty() || line.startsWith("*")) {
                    continue;
                }
                
                String[] parts = line.split("\\s+");
                if (parts.length >= 2 && "device".equals(parts[1])) {
                    deviceComboBox.addItem(parts[0]);
                    deviceCount++;
                    logInfo("发现设备: " + parts[0]);
                }
            }
            
            reader.close();
            process.waitFor();
            
            if (deviceComboBox.getItemCount() == 0) {
                deviceComboBox.addItem("未检测到设备");
                setStatus("未检测到已连接的设备");
                logWarning("未检测到任何设备");
            } else {
                setStatus("检测到 " + deviceComboBox.getItemCount() + " 个设备");
                logSuccess("共检测到 " + deviceCount + " 个设备");
            }
            
        } catch (Exception e) {
            logError("检测设备失败: " + e.getMessage());
            showError("检测设备失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 加载应用列表
     */
    private void loadApps() {
        String device = (String) deviceComboBox.getSelectedItem();
        if (device == null || "未检测到设备".equals(device)) {
            logWarning("尝试加载应用但未选择有效设备");
            showWarning("请先选择有效的设备");
            return;
        }
        
        logInfo("开始加载设备 [" + device + "] 的应用列表...");
        setStatus("正在加载应用列表...");
        allApps.clear();
        userAppsMap.clear();
        
        // 清空表格
        user0Model.setRowCount(0);
        otherUserModel.setRowCount(0);
        
        try {
            // 获取所有用户ID
            List<Integer> userIds = getUserIds(device);
            logInfo("发现 " + userIds.size() + " 个用户: " + userIds);
            
            // 为每个用户获取第三方应用
            int totalApps = 0;
            for (int userId : userIds) {
                List<AppInfo> apps = getThirdPartyApps(device, userId);
                userAppsMap.put(userId, apps);
                allApps.addAll(apps);
                totalApps += apps.size();
                logInfo("用户 " + userId + " 有 " + apps.size() + " 个第三方应用");
            }
            
            // 更新表格（分左右栏显示）
            updateTables();
            setStatus("已加载 " + allApps.size() + " 个应用（" + userIds.size() + " 个用户）");
            logSuccess("共加载 " + totalApps + " 个应用");
            
        } catch (Exception e) {
            logError("加载应用列表失败: " + e.getMessage());
            showError("加载应用列表失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 获取设备上的用户ID列表
     */
    private List<Integer> getUserIds(String device) throws Exception {
        List<Integer> userIds = new ArrayList<>();
        
        Process process = Runtime.getRuntime().exec(
            new String[]{"adb", "-s", device, "shell", "pm", "list", "users"}
        );
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        
        String line;
        while ((line = reader.readLine()) != null) {
            // 解析类似: UserInfo{0:机主:c13} running
            if (line.contains("UserInfo")) {
                try {
                    int start = line.indexOf('{') + 1;
                    int end = line.indexOf(':');
                    if (start > 0 && end > start) {
                        int userId = Integer.parseInt(line.substring(start, end).trim());
                        userIds.add(userId);
                    }
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
        }
        
        reader.close();
        process.waitFor();
        
        // 如果没有找到用户，默认添加用户0
        if (userIds.isEmpty()) {
            userIds.add(0);
        }
        
        return userIds;
    }
    
    /**
     * 获取指定用户的第三方应用
     */
    private List<AppInfo> getThirdPartyApps(String device, int userId) throws Exception {
        List<AppInfo> apps = new ArrayList<>();
        
        Process process = Runtime.getRuntime().exec(
            new String[]{"adb", "-s", device, "shell", "pm", "list", "packages", "-3", "--user", String.valueOf(userId)}
        );
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        
        String line;
        while ((line = reader.readLine()) != null) {
            // 解析类似: package:com.example.app
            if (line.startsWith("package:")) {
                String packageName = line.substring(8).trim();
                apps.add(new AppInfo(packageName, userId));
            }
        }
        
        reader.close();
        process.waitFor();
        
        return apps;
    }
    
    /**
     * 更新表格显示（分左右栏）
     */
    private void updateTables() {
        // 清空表格
        user0Model.setRowCount(0);
        otherUserModel.setRowCount(0);
        
        // 左栏：用户0的应用
        List<AppInfo> user0Apps = userAppsMap.getOrDefault(0, new ArrayList<>());
        for (AppInfo app : user0Apps) {
            String type = app.isHuidoudour ? "⭐ huidoudour" : "第三方";
            user0Model.addRow(new Object[]{app.packageName, type});
        }
        
        // 右栏：其他用户的应用
        for (Map.Entry<Integer, List<AppInfo>> entry : userAppsMap.entrySet()) {
            if (entry.getKey() != 0) {
                for (AppInfo app : entry.getValue()) {
                    String type = app.isHuidoudour ? "⭐ huidoudour" : "第三方";
                    otherUserModel.addRow(new Object[]{app.packageName + " [用户" + app.userId + "]", type});
                }
            }
        }
    }
    
    /**
     * 获取选中的应用
     */
    private AppInfo getSelectedApp(JTable table, boolean isUser0) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            showWarning("请先选择一个应用");
            return null;
        }
        
        List<AppInfo> apps = isUser0 ? 
            userAppsMap.getOrDefault(0, new ArrayList<>()) :
            getAllOtherUserApps();
        
        if (selectedRow < apps.size()) {
            return apps.get(selectedRow);
        }
        
        return null;
    }
    
    /**
     * 获取所有其他用户的应用
     */
    private List<AppInfo> getAllOtherUserApps() {
        List<AppInfo> result = new ArrayList<>();
        for (Map.Entry<Integer, List<AppInfo>> entry : userAppsMap.entrySet()) {
            if (entry.getKey() != 0) {
                result.addAll(entry.getValue());
            }
        }
        return result;
    }
    
    /**
     * 启动应用（智能降级策略）
     */
    private void launchApp(AppInfo app) {
        String device = (String) deviceComboBox.getSelectedItem();
        if (device == null || "未检测到设备".equals(device)) {
            logWarning("尝试启动应用但未选择有效设备");
            showWarning("请先选择有效的设备");
            return;
        }
        
        setStatus("正在启动应用...");
        logInfo("========== 开始启动应用: " + app.packageName + " ==========");
        
        // 方法1：优先使用 am start 命令（更标准、更可靠）
        logInfo("【方法1】尝试使用 am start 启动应用...");
        boolean success = launchWithAmStart(device, app);
        
        if (!success) {
            // 方法1失败，降级到方法2：使用 monkey 命令
            logWarning("【方法1】am start 启动失败，尝试降级方案...");
            logInfo("【方法2】尝试使用 monkey 启动应用...");
            success = launchWithMonkey(device, app);
        }
        
        if (success) {
            String msg = "应用启动成功";
            logSuccess(msg + " (" + app.packageName + ")");
            showSuccess(msg);
        } else {
            logError("所有启动方法均失败");
            showError("应用启动失败，请检查应用是否正确安装且有LAUNCHER Activity");
        }
        
        logInfo("========== 启动流程结束 ==========");
    }
    
    /**
     * 方法1：使用 am start 启动应用（优先方案）
     */
    private boolean launchWithAmStart(String device, AppInfo app) {
        try {
            // 首先获取应用的启动Activity
            logInfo("步骤1: 查询应用的启动Activity...");
            String launchActivity = getLaunchActivity(device, app.packageName);
            
            if (launchActivity == null || launchActivity.isEmpty()) {
                logWarning("无法获取启动Activity，此方法可能失败");
                // 尝试使用包名直接启动
                logInfo("步骤2: 尝试使用包名直接启动...");
                String[] adbCommand = new String[]{
                    "adb", "-s", device, "shell", "am", "start", 
                    "-a", "android.intent.action.MAIN",
                    "-c", "android.intent.category.LAUNCHER",
                    "-p", app.packageName
                };
                return executeLaunchCommand(adbCommand, "am start (包名方式)");
            } else {
                logInfo("步骤2: 找到启动Activity: " + launchActivity);
                // 使用完整的Activity名称启动
                logInfo("步骤3: 使用完整Activity路径启动...");
                String[] adbCommand = new String[]{
                    "adb", "-s", device, "shell", "am", "start",
                    "-n", launchActivity
                };
                return executeLaunchCommand(adbCommand, "am start (Activity方式)");
            }
            
        } catch (Exception e) {
            logError("am start 启动异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取应用的启动Activity
     */
    private String getLaunchActivity(String device, String packageName) {
        try {
            String[] adbCommand = new String[]{
                "adb", "-s", device, "shell", "cmd", "package", "resolve-activity", "--brief", packageName
            };
            
            Process process = Runtime.getRuntime().exec(adbCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            reader.close();
            process.waitFor();
            
            String result = output.toString().trim();
            logInfo("Activity查询结果: " + result);
            
            // 解析结果，提取Activity名称
            if (result.contains("/") && !result.contains("No activity found")) {
                // 格式通常是: com.example.app/.MainActivity
                return result;
            }
            
            return null;
            
        } catch (Exception e) {
            logWarning("查询Activity失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 执行启动命令并检查结果
     */
    private boolean executeLaunchCommand(String[] adbCommand, String methodName) {
        try {
            logInfo("执行命令: " + String.join(" ", adbCommand));
            
            Process process = Runtime.getRuntime().exec(adbCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            
            reader.close();
            errorReader.close();
            process.waitFor();
            
            String result = output.toString();
            String errorResult = errorOutput.toString();
            
            logInfo(methodName + " - ADB输出: " + result.trim());
            if (!errorResult.isEmpty()) {
                logWarning(methodName + " - 错误输出: " + errorResult.trim());
            }
            
            // 判断是否成功
            if (result.contains("Starting") || 
                result.contains("Success") ||
                (!errorResult.contains("Error") && !errorResult.contains("Exception"))) {
                logInfo(methodName + " - 启动成功");
                return true;
            } else {
                logWarning(methodName + " - 启动可能失败");
                return false;
            }
            
        } catch (Exception e) {
            logError(methodName + " 执行异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 方法2：使用 monkey 启动应用（降级方案）
     */
    private boolean launchWithMonkey(String device, AppInfo app) {
        try {
            String[] adbCommand = new String[]{
                "adb", "-s", device, "shell", "monkey", 
                "-p", app.packageName, 
                "-c", "android.intent.category.LAUNCHER", 
                "1"
            };
            
            logInfo("执行命令: " + String.join(" ", adbCommand));
            
            Process process = Runtime.getRuntime().exec(adbCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            
            reader.close();
            errorReader.close();
            process.waitFor();
            
            String result = output.toString();
            String errorResult = errorOutput.toString();
            
            logInfo("monkey - ADB输出: " + result.trim());
            if (!errorResult.isEmpty()) {
                logWarning("monkey - 错误输出: " + errorResult.trim());
            }
            
            // monkey成功的标志
            if (result.contains("Events injected") || 
                result.contains(":Dropped") ||
                (!errorResult.contains("No activities found") && !errorResult.contains("Security exception"))) {
                logInfo("monkey - 启动成功");
                return true;
            } else {
                logWarning("monkey - 启动可能失败");
                return false;
            }
            
        } catch (Exception e) {
            logError("monkey 启动异常: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 强制停止应用
     */
    private void forceStopApp(AppInfo app) {
        String device = (String) deviceComboBox.getSelectedItem();
        if (device == null || "未检测到设备".equals(device)) {
            logWarning("尝试强制停止应用但未选择有效设备");
            showWarning("请先选择有效的设备");
            return;
        }
        
        // 二次确认
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "确定要强制停止用户" + app.userId + "的应用 \"" + app.packageName + "\" 吗？\n\n" +
            "这将立即停止应用的所有活动，可能导致数据丢失。",
            "确认强制停止",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (confirm != JOptionPane.YES_OPTION) {
            logInfo("用户取消了强制停止操作");
            return;
        }
        
        setStatus("正在强制停止应用...");
        String[] adbCommand = new String[]{"adb", "-s", device, "shell", "am", "force-stop", app.packageName};
        logInfo("执行强制停止命令: " + String.join(" ", adbCommand));
        
        try {
            Process process = Runtime.getRuntime().exec(adbCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            reader.close();
            process.waitFor();
            
            String result = output.toString();
            logInfo("ADB输出: " + result.trim());
            
            String msg = "应用已强制停止";
            logSuccess(msg + " (" + app.packageName + ")");
            showSuccess(msg);
            
        } catch (Exception e) {
            logError("强制停止应用异常: " + e.getMessage());
            showError("强制停止应用失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 杀死应用进程
     */
    private void killAppProcess(AppInfo app) {
        String device = (String) deviceComboBox.getSelectedItem();
        if (device == null || "未检测到设备".equals(device)) {
            logWarning("尝试杀死进程但未选择有效设备");
            showWarning("请先选择有效的设备");
            return;
        }
        
        // 二次确认
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "警告：此操作较为激进！\n\n" +
            "确定要杀死用户" + app.userId + "的应用 \"" + app.packageName + "\" 的所有进程吗？\n" +
            "这会比强制停止更彻底，但可能导致系统不稳定。",
            "确认杀死进程",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.ERROR_MESSAGE
        );
        
        if (confirm != JOptionPane.YES_OPTION) {
            logInfo("用户取消了杀死进程操作");
            return;
        }
        
        setStatus("正在杀死应用进程...");
        // 使用kill命令杀死进程
        String[] adbCommand = new String[]{"adb", "-s", device, "shell", "pkill", "-f", app.packageName};
        logInfo("执行杀死进程命令: " + String.join(" ", adbCommand));
        
        try {
            Process process = Runtime.getRuntime().exec(adbCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            reader.close();
            process.waitFor();
            
            String result = output.toString();
            logInfo("ADB输出: " + result.trim());
            
            String msg = "应用进程已杀死";
            logSuccess(msg + " (" + app.packageName + ")");
            showSuccess(msg);
            
        } catch (Exception e) {
            logError("杀死应用进程异常: " + e.getMessage());
            showError("杀死应用进程失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 卸载应用
     */
    private void uninstallApp(AppInfo app) {
        String device = (String) deviceComboBox.getSelectedItem();
        if (device == null || "未检测到设备".equals(device)) {
            logWarning("尝试卸载应用但未选择有效设备");
            showWarning("请先选择有效的设备");
            return;
        }
        
        // 智能卸载逻辑
        String confirmMessage;
        String[] adbCommand;
        
        if (app.userId == 0) {
            // 主用户0的应用：为所有用户卸载
            confirmMessage = "这是主用户（用户0）的应用\n\n" +
                           "确定要为【所有用户】卸载应用 \"" + app.packageName + "\" 吗？\n" +
                           "此操作将从所有用户空间中删除该应用，不可恢复！";
            adbCommand = new String[]{"adb", "-s", device, "uninstall", app.packageName};
            logInfo("准备卸载应用 (所有用户): " + app.packageName);
        } else {
            // 非主用户的应用：仅卸载当前用户
            confirmMessage = "这是用户 " + app.userId + " 的应用\n\n" +
                           "确定要仅为【用户" + app.userId + "】卸载应用 \"" + app.packageName + "\" 吗？\n" +
                           "其他用户的应用将不受影响。";
            adbCommand = new String[]{"adb", "-s", device, "shell", "pm", "uninstall", "--user", String.valueOf(app.userId), app.packageName};
            logInfo("准备卸载应用 (用户" + app.userId + "): " + app.packageName);
        }
        
        // 二次确认
        int confirm = JOptionPane.showConfirmDialog(
            this,
            confirmMessage,
            "确认卸载",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (confirm != JOptionPane.YES_OPTION) {
            logInfo("用户取消了卸载操作");
            return;
        }
        
        setStatus("正在卸载应用...");
        logInfo("执行命令: " + String.join(" ", adbCommand));
        
        try {
            Process process = Runtime.getRuntime().exec(adbCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            reader.close();
            process.waitFor();
            
            String result = output.toString();
            logInfo("ADB输出: " + result.trim());
            
            if (result.contains("Success")) {
                String successMsg = app.userId == 0 ? 
                    "应用已从所有用户中卸载成功" : 
                    "应用已从用户" + app.userId + "中卸载成功";
                logSuccess(successMsg + " (" + app.packageName + ")");
                showSuccess(successMsg);
                loadApps();
            } else {
                logError("卸载失败: " + result);
                showError("卸载失败: " + result);
            }
            
        } catch (Exception e) {
            logError("卸载应用异常: " + e.getMessage());
            showError("卸载应用失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 清除应用数据
     */
    private void clearData(AppInfo app) {
        String device = (String) deviceComboBox.getSelectedItem();
        if (device == null || "未检测到设备".equals(device)) {
            logWarning("尝试清除数据但未选择有效设备");
            showWarning("请先选择有效的设备");
            return;
        }
        
        // 二次确认
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "警告：此操作不可恢复！\n\n" +
            "确定要清除用户" + app.userId + "的应用 \"" + app.packageName + "\" 的所有数据吗？\n" +
            "包括：登录信息、设置、本地文件等",
            "确认清除数据",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (confirm != JOptionPane.YES_OPTION) {
            logInfo("用户取消了清除数据操作");
            return;
        }
        
        setStatus("正在清除应用数据...");
        String[] adbCommand = new String[]{"adb", "-s", device, "shell", "pm", "clear", "--user", String.valueOf(app.userId), app.packageName};
        logInfo("执行清除数据命令: " + String.join(" ", adbCommand));
        
        try {
            Process process = Runtime.getRuntime().exec(adbCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            reader.close();
            process.waitFor();
            
            String result = output.toString();
            logInfo("ADB输出: " + result.trim());
            
            if (result.contains("Success") || result.contains("success")) {
                String msg = "用户" + app.userId + "的应用数据清除成功";
                logSuccess(msg + " (" + app.packageName + ")");
                showSuccess(msg);
            } else {
                logError("清除数据失败: " + result);
                showError("清除数据失败: " + result);
            }
            
        } catch (Exception e) {
            logError("清除应用数据异常: " + e.getMessage());
            showError("清除应用数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 清除应用缓存（使用正确的命令）
     */
    private void clearCache(AppInfo app) {
        String device = (String) deviceComboBox.getSelectedItem();
        if (device == null || "未检测到设备".equals(device)) {
            logWarning("尝试清除缓存但未选择有效设备");
            showWarning("请先选择有效的设备");
            return;
        }
        
        // 二次确认
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "确定要清除用户" + app.userId + "的应用 \"" + app.packageName + "\" 的缓存吗？\n\n" +
            "注意：这将只清除缓存文件，不会删除应用数据",
            "确认清除缓存",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (confirm != JOptionPane.YES_OPTION) {
            logInfo("用户取消了清除缓存操作");
            return;
        }
        
        setStatus("正在清除应用缓存...");
        // 使用正确的清除缓存命令：am trim-caches 或通过包管理器清理
        String[] adbCommand = new String[]{"adb", "-s", device, "shell", "pm", "trim-caches", "999G"};
        logInfo("执行清除缓存命令: " + String.join(" ", adbCommand));
        logInfo("注意：Android系统会清除所有应用的缓存，包括目标应用");
        
        try {
            Process process = Runtime.getRuntime().exec(adbCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            reader.close();
            process.waitFor();
            
            String result = output.toString();
            logInfo("ADB输出: " + result.trim());
            
            // trim-caches命令通常返回空或简单确认
            String msg = "用户" + app.userId + "的应用缓存已清除";
            logSuccess(msg + " (" + app.packageName + ")");
            showSuccess(msg + "\n注意：系统清除了所有应用的缓存以释放空间");
            
        } catch (Exception e) {
            logError("清除应用缓存异常: " + e.getMessage());
            showError("清除应用缓存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 设置状态栏文本
     */
    private void setStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
        });
    }
    
    /**
     * 显示错误对话框
     */
    private void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                this,
                message,
                "操作失败",
                JOptionPane.PLAIN_MESSAGE
            );
            setStatus("操作失败");
        });
    }
    
    /**
     * 显示警告对话框
     */
    private void showWarning(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                this,
                message,
                "警告",
                JOptionPane.PLAIN_MESSAGE
            );
        });
    }
    
    /**
     * 显示成功对话框
     */
    private void showSuccess(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                this,
                message,
                "操作成功",
                JOptionPane.PLAIN_MESSAGE
            );
            setStatus("操作成功");
        });
    }
    
    /**
     * 主方法 - 程序入口
     */
    public static void main(String[] args) {
        System.out.println("===========================================================");
        System.out.println("       Android应用管理工具 v3.0 - 慧兜兜专用版");
        System.out.println("       仅用于学习和研究，不建议在生产环境中使用");
        System.out.println("===========================================================");
        System.out.println();
        
        // 设置系统外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // 在事件调度线程中启动GUI
        SwingUtilities.invokeLater(() -> {
            try {
                AndroidAppManager manager = new AndroidAppManager();
                manager.setVisible(true);
                
                // 输出窗口实际尺寸信息
                java.awt.Dimension size = manager.getSize();
                java.awt.Dimension contentSize = manager.getContentPane().getSize();
                System.out.println("GUI界面已启动");
                System.out.println("窗口外部尺寸（包括标题栏和边框）: " + size.width + "x" + size.height);
                System.out.println("内容区域尺寸: " + contentSize.width + "x" + contentSize.height);
                System.out.println("提示：所有操作日志将在此控制台输出\n");
            } catch (Exception e) {
                System.err.println("启动失败: " + e.getMessage());
                JOptionPane.showMessageDialog(
                    null,
                    "启动失败: " + e.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE
                );
                e.printStackTrace();
            }
        });
    }
    
    /**
     * 圆角边框类
     */
    static class RoundBorder implements javax.swing.border.Border {
        private Color color;
        private int radius;
        
        public RoundBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }
        
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(color);
            g2d.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2d.dispose();
        }
        
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(2, 2, 2, 2);
        }
        
        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
}
