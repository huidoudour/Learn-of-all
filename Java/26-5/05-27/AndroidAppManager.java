// Android应用管理工具
// 仅由 huidoudour(慧兜兜)学习使用
// 该程序仅用于学习和研究，不建议在生产环境中使用
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Android应用管理工具
 * 通过ADB管理Android设备和AVD上的应用
 * 
 * @author huidoudour
 * @version 1.0
 */
public class AndroidAppManager extends JFrame {
    
    // UI组件
    private JComboBox<String> deviceComboBox;
    private JTable appTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;
    private JButton uninstallButton;
    private JButton clearDataButton;
    private JButton clearCacheButton;
    private JLabel statusLabel;
    
    // 数据存储
    private List<AppInfo> appList;
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
            return packageName + (isHuidoudour ? " [⭐huidoudour]" : "");
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
        setTitle("Android应用管理工具");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 顶部控制面板
        JPanel topPanel = createTopPanel();
        mainPanel.add(topPanel, BorderLayout.NORTH);
        
        // 中间表格面板
        JPanel centerPanel = createCenterPanel();
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        // 底部操作按钮面板
        JPanel bottomPanel = createBottomPanel();
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        // 状态栏
        statusLabel = new JLabel("就绪");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        mainPanel.add(statusLabel, BorderLayout.PAGE_END);
        
        add(mainPanel);
        
        // 初始化数据
        appList = new ArrayList<>();
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
        
        refreshButton = new JButton("刷新设备");
        refreshButton.addActionListener(e -> loadDevices());
        panel.add(refreshButton);
        
        JButton loadAppsButton = new JButton("加载应用");
        loadAppsButton.addActionListener(e -> loadApps());
        panel.add(loadAppsButton);
        
        return panel;
    }
    
    /**
     * 创建中间表格面板
     */
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("应用列表"));
        
        // 表格模型
        String[] columns = {"用户ID", "包名", "类型"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 表格不可编辑
            }
        };
        
        appTable = new JTable(tableModel);
        appTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        appTable.setRowHeight(25);
        appTable.getTableHeader().setReorderingAllowed(false);
        
        // 添加滚动条
        JScrollPane scrollPane = new JScrollPane(appTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建底部操作按钮面板
     */
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        panel.setBorder(BorderFactory.createTitledBorder("应用操作"));
        
        uninstallButton = new JButton("卸载应用");
        uninstallButton.addActionListener(e -> uninstallApp());
        panel.add(uninstallButton);
        
        clearDataButton = new JButton("清除数据");
        clearDataButton.addActionListener(e -> clearData());
        panel.add(clearDataButton);
        
        clearCacheButton = new JButton("清除缓存");
        clearCacheButton.addActionListener(e -> clearCache());
        panel.add(clearCacheButton);
        
        return panel;
    }
    
    /**
     * 加载已连接的设备
     */
    private void loadDevices() {
        setStatus("正在检测设备...");
        deviceComboBox.removeAllItems();
        
        try {
            Process process = Runtime.getRuntime().exec("adb devices");
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
        appList.clear();
        userAppsMap.clear();
        tableModel.setRowCount(0);
        
        try {
            // 获取所有用户ID
            List<Integer> userIds = getUserIds(device);
            
            // 为每个用户获取第三方应用
            for (int userId : userIds) {
                List<AppInfo> apps = getThirdPartyApps(device, userId);
                userAppsMap.put(userId, apps);
                appList.addAll(apps);
            }
            
            // 更新表格
            updateTable();
            setStatus("已加载 " + appList.size() + " 个应用（" + userIds.size() + " 个用户）");
            
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
            "adb -s " + device + " shell pm list users"
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
            "adb -s " + device + " shell pm list packages -3 --user " + userId
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
     * 更新表格显示
     */
    private void updateTable() {
        tableModel.setRowCount(0);
        
        for (AppInfo app : appList) {
            String type = app.isHuidoudour ? "⭐ huidoudour应用" : "第三方应用";
            tableModel.addRow(new Object[]{
                app.userId,
                app.packageName,
                type
            });
        }
    }
    
    /**
     * 获取选中的应用
     */
    private AppInfo getSelectedApp() {
        int selectedRow = appTable.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }
        
        if (selectedRow < appList.size()) {
            return appList.get(selectedRow);
        }
        
        return null;
    }
    
    /**
     * 卸载应用
     */
    private void uninstallApp() {
        AppInfo app = getSelectedApp();
        if (app == null) {
            showWarning("请先选择一个应用");
            return;
        }
        
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
                "adb -s " + device + " uninstall " + app.packageName
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
                loadApps(); // 重新加载应用列表
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
    private void clearData() {
        AppInfo app = getSelectedApp();
        if (app == null) {
            showWarning("请先选择一个应用");
            return;
        }
        
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
                "adb -s " + device + " shell pm clear " + app.packageName
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
    private void clearCache() {
        AppInfo app = getSelectedApp();
        if (app == null) {
            showWarning("请先选择一个应用");
            return;
        }
        
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
            // 使用pm trim-caches命令清除所有缓存，或者使用特定方法
            // 注意：Android没有直接清除单个应用缓存的命令，这里使用变通方法
            Process process = Runtime.getRuntime().exec(
                "adb -s " + device + " shell pm clear " + app.packageName
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
