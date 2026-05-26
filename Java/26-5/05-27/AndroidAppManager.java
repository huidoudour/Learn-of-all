// Android应用管理工具
// 仅由 huidoudour(慧兜兜)学习使用
// 该程序仅用于学习和研究，不建议在生产环境中使用
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
 * @version 2.0
 */
public class AndroidAppManager extends JFrame {
    
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
        setTitle("Android应用管理工具 - 双用户视图");
        setSize(1400, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 顶部控制面板
        JPanel topPanel = createTopPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);
        
        // 中间双栏面板（左右分栏）
        JPanel centerPanel = createDualColumnPanel();
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        // 底部状态栏
        statusLabel = new JLabel("就绪 | 提示：右键点击应用可进行操作");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mainPanel.add(statusLabel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // 初始化数据
        allApps = new ArrayList<>();
        userAppsMap = new HashMap<>();
    }
    
    /**
     * 创建顶部控制面板
     */
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("设备选择"));
        
        panel.add(new JLabel("选择设备:"));
        deviceComboBox = new JComboBox<>();
        deviceComboBox.setPreferredSize(new Dimension(300, 25));
        panel.add(deviceComboBox);
        
        refreshButton = new JButton("🔄 刷新设备");
        refreshButton.addActionListener(e -> loadDevices());
        panel.add(refreshButton);
        
        JButton loadAppsButton = new JButton("📱 加载应用");
        loadAppsButton.addActionListener(e -> loadApps());
        panel.add(loadAppsButton);
        
        return panel;
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
     * 创建用户应用面板
     */
    private JPanel createUserPanel(String title, boolean isUser0) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        
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
        
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(25);
        table.getTableHeader().setReorderingAllowed(false);
        
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
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 底部提示
        JLabel hintLabel = new JLabel("💡 右键或双击应用进行操作");
        hintLabel.setHorizontalAlignment(SwingConstants.CENTER);
        hintLabel.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        hintLabel.setForeground(Color.GRAY);
        panel.add(hintLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * 添加右键上下文菜单
     */
    private void addContextMenu(JTable table, boolean isUser0) {
        JPopupMenu popupMenu = new JPopupMenu();
        
        JMenuItem uninstallItem = new JMenuItem("🗑️ 卸载应用");
        uninstallItem.addActionListener(e -> {
            AppInfo app = getSelectedApp(table, isUser0);
            if (app != null) {
                uninstallApp(app);
            }
        });
        popupMenu.add(uninstallItem);
        
        JMenuItem clearDataItem = new JMenuItem("🧹 清除数据");
        clearDataItem.addActionListener(e -> {
            AppInfo app = getSelectedApp(table, isUser0);
            if (app != null) {
                clearData(app);
            }
        });
        popupMenu.add(clearDataItem);
        
        JMenuItem clearCacheItem = new JMenuItem("📦 清除缓存");
        clearCacheItem.addActionListener(e -> {
            AppInfo app = getSelectedApp(table, isUser0);
            if (app != null) {
                clearCache(app);
            }
        });
        popupMenu.add(clearCacheItem);
        
        popupMenu.addSeparator();
        
        JMenuItem copyPackageItem = new JMenuItem("📋 复制包名");
        copyPackageItem.addActionListener(e -> {
            AppInfo app = getSelectedApp(table, isUser0);
            if (app != null) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new java.awt.datatransfer.StringSelection(app.packageName), null);
                setStatus("已复制包名: " + app.packageName);
            }
        });
        popupMenu.add(copyPackageItem);
        
        table.setComponentPopupMenu(popupMenu);
    }
    
    /**
     * 显示应用操作对话框
     */
    private void showAppOperations(JTable table, boolean isUser0) {
        AppInfo app = getSelectedApp(table, isUser0);
        if (app == null) {
            return;
        }
        
        String[] options = {"卸载应用", "清除数据", "清除缓存", "取消"};
        int choice = JOptionPane.showOptionDialog(
            this,
            "选择对 \"" + app.packageName + "\" 的操作：",
            "应用操作",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]
        );
        
        switch (choice) {
            case 0:
                uninstallApp(app);
                break;
            case 1:
                clearData(app);
                break;
            case 2:
                clearCache(app);
                break;
        }
    }
    
    /**
     * 加载已连接的设备
     */
    private void loadDevices() {
        setStatus("正在检测设备...");
        deviceComboBox.removeAllItems();
        
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"adb", "devices"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            String line;
            boolean firstLine = true;
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
                }
            }
            
            reader.close();
            process.waitFor();
            
            if (deviceComboBox.getItemCount() == 0) {
                deviceComboBox.addItem("未检测到设备");
                setStatus("未检测到已连接的设备");
            } else {
                setStatus("检测到 " + deviceComboBox.getItemCount() + " 个设备");
            }
            
        } catch (Exception e) {
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
            showWarning("请先选择有效的设备");
            return;
        }
        
        setStatus("正在加载应用列表...");
        allApps.clear();
        userAppsMap.clear();
        
        // 清空表格
        user0Model.setRowCount(0);
        otherUserModel.setRowCount(0);
        
        try {
            // 获取所有用户ID
            List<Integer> userIds = getUserIds(device);
            
            // 为每个用户获取第三方应用
            for (int userId : userIds) {
                List<AppInfo> apps = getThirdPartyApps(device, userId);
                userAppsMap.put(userId, apps);
                allApps.addAll(apps);
            }
            
            // 更新表格（分左右栏显示）
            updateTables();
            setStatus("已加载 " + allApps.size() + " 个应用（" + userIds.size() + " 个用户）");
            
        } catch (Exception e) {
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
     * 卸载应用
     */
    private void uninstallApp(AppInfo app) {
        // 二次确认
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "确定要卸载应用 \"" + app.packageName + "\" 吗？\n此操作不可恢复！",
            "确认卸载",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        String device = (String) deviceComboBox.getSelectedItem();
        if (device == null || "未检测到设备".equals(device)) {
            showWarning("请先选择有效的设备");
            return;
        }
        
        setStatus("正在卸载应用...");
        
        try {
            Process process = Runtime.getRuntime().exec(
                new String[]{"adb", "-s", device, "uninstall", app.packageName}
            );
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            reader.close();
            process.waitFor();
            
            String result = output.toString();
            if (result.contains("Success")) {
                showSuccess("应用卸载成功！");
                loadApps();
            } else {
                showError("卸载失败: " + result);
            }
            
        } catch (Exception e) {
            showError("卸载应用失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 清除应用数据
     */
    private void clearData(AppInfo app) {
        // 二次确认
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "确定要清除应用 \"" + app.packageName + "\" 的所有数据吗？\n包括登录信息、设置等，此操作不可恢复！",
            "确认清除数据",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        String device = (String) deviceComboBox.getSelectedItem();
        if (device == null || "未检测到设备".equals(device)) {
            showWarning("请先选择有效的设备");
            return;
        }
        
        setStatus("正在清除应用数据...");
        
        try {
            Process process = Runtime.getRuntime().exec(
                new String[]{"adb", "-s", device, "shell", "pm", "clear", app.packageName}
            );
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            reader.close();
            process.waitFor();
            
            String result = output.toString();
            if (result.contains("Success") || result.contains("success")) {
                showSuccess("应用数据清除成功！");
            } else {
                showError("清除数据失败: " + result);
            }
            
        } catch (Exception e) {
            showError("清除应用数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 清除应用缓存
     */
    private void clearCache(AppInfo app) {
        // 二次确认
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "确定要清除应用 \"" + app.packageName + "\" 的缓存吗？",
            "确认清除缓存",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        
        String device = (String) deviceComboBox.getSelectedItem();
        if (device == null || "未检测到设备".equals(device)) {
            showWarning("请先选择有效的设备");
            return;
        }
        
        setStatus("正在清除应用缓存...");
        
        try {
            Process process = Runtime.getRuntime().exec(
                new String[]{"adb", "-s", device, "shell", "pm", "clear", app.packageName}
            );
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            reader.close();
            process.waitFor();
            
            String result = output.toString();
            if (result.contains("Success") || result.contains("success")) {
                showSuccess("应用缓存清除成功！\n注意：此操作也会清除应用数据");
            } else {
                showError("清除缓存失败: " + result);
            }
            
        } catch (Exception e) {
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
                "错误",
                JOptionPane.ERROR_MESSAGE
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
                JOptionPane.WARNING_MESSAGE
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
                "成功",
                JOptionPane.INFORMATION_MESSAGE
            );
            setStatus("操作成功");
        });
    }
    
    /**
     * 主方法 - 程序入口
     */
    public static void main(String[] args) {
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
            } catch (Exception e) {
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
}
