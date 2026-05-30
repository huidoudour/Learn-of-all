// 批量快速Ping工具
// 仅由 huidoudour(慧兜兜)学习使用
// 该程序仅用于学习和研究，不建议在生产环境中使用
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * 批量快速Ping工具
 * 支持同时ping多个IP/域名，支持网段IP范围扫描
 * 深色主题UI，线程池并发执行
 *
 * @author huidoudour
 * @version 2.2
 */
public class BatchPingTool extends JFrame {

    // ========================================
    //  配色方案 - 深邃午夜蓝主题
    // ========================================
    private static final Color BG_DARK       = new Color(22, 24, 30);
    private static final Color BG_CARD       = new Color(32, 34, 42);
    private static final Color BG_INPUT      = new Color(40, 43, 52);
    private static final Color ACCENT_BLUE   = new Color(82, 139, 255);
    private static final Color ACCENT_CYAN   = new Color(45, 210, 200);
    private static final Color SUCCESS_GREEN = new Color(46, 204, 113);
    private static final Color FAIL_RED      = new Color(255, 86, 86);
    private static final Color WARN_AMBER    = new Color(255, 184, 77);
    private static final Color TEXT_PRIMARY  = new Color(220, 223, 232);
    private static final Color TEXT_SECONDARY= new Color(140, 145, 158);
    private static final Color TEXT_MUTED    = new Color(100, 105, 118);
    private static final Color BORDER_SUBTLE = new Color(48, 51, 62);
    private static final Color ROW_EVEN      = new Color(36, 38, 48);
    private static final Color ROW_ODD       = new Color(30, 32, 40);
    private static final Color PROGRESS_BG   = new Color(42, 45, 55);

    // ========================================
    //  字体
    // ========================================
    private static final Font FONT_TITLE       = new Font("Microsoft YaHei UI", Font.BOLD, 20);
    private static final Font FONT_SUBTITLE    = new Font("Microsoft YaHei UI", Font.PLAIN, 12);
    private static final Font FONT_BUTTON      = new Font("Microsoft YaHei UI", Font.BOLD, 13);
    private static final Font FONT_MONO        = new Font("Consolas", Font.PLAIN, 13);
    private static final Font FONT_TABLE       = new Font("Microsoft YaHei UI", Font.PLAIN, 13);
    private static final Font FONT_TABLE_HEADER= new Font("Microsoft YaHei UI", Font.BOLD, 12);
    private static final Font FONT_STAT_NUM    = new Font("Consolas", Font.BOLD, 22);
    private static final Font FONT_STAT_LABEL  = new Font("Microsoft YaHei UI", Font.PLAIN, 11);
    private static final Font FONT_SECTION     = new Font("Microsoft YaHei UI", Font.BOLD, 13);
    private static final Font FONT_OCTET       = new Font("Consolas", Font.BOLD, 14);

    // ========================================
    //  UI组件
    // ========================================
    private JTextArea inputArea;
    private OctetField[] startOctets;  // 起始IP四段
    private OctetField[] endOctets;    // 终止IP四段
    private RoundedButton startBtn;
    private RoundedButton stopBtn;
    private RoundedButton clearBtn;
    private RoundedButton exportBtn;
    private RoundedButton addPresetBtn;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private GradientProgressBar progressBar;
    private JLabel totalCountLabel;
    private JLabel successCountLabel;
    private JLabel failCountLabel;
    private JLabel avgTimeLabel;
    private JLabel elapsedLabel;

    // ========================================
    //  数据 & 控制
    // ========================================
    private final List<PingTarget> targets = new ArrayList<>();
    private final Map<String, PingResult> resultMap = new ConcurrentHashMap<>();
    private final Map<Integer, String> rowToHostMap = new ConcurrentHashMap<>();
    private ExecutorService executor;
    private java.util.concurrent.ScheduledExecutorService timerExecutor;
    private volatile boolean running = false;
    private volatile boolean stopRequested = false;
    private final AtomicInteger completedCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failCount = new AtomicInteger(0);
    private long startTime;
    private final List<Long> responseTimes = new ArrayList<>();
    private final Object responseTimesLock = new Object();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    // 状态常量（纯文本，无emoji）
    private static final String STATUS_SUCCESS = "成功";
    private static final String STATUS_FAIL    = "失败";
    private static final String STATUS_TIMEOUT = "超时";
    private static final String STATUS_RUNNING = "探测中";
    private static final String STATUS_PENDING = "等待";

    /**
     * Ping目标
     */
    private static class PingTarget {
        String host;
        PingTarget(String host) {
            this.host = host;
        }
    }

    /**
     * Ping结果
     */
    private static class PingResult {
        String host;
        String ip;
        String status;
        String responseTime;
        String ttl;
        String sent;
        String received;
        String loss;
        String timestamp;
    }

    // ========================================
    //  入口
    // ========================================
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException |
                 IllegalAccessException | UnsupportedLookAndFeelException ignored) {}

        SwingUtilities.invokeLater(() -> {
            BatchPingTool tool = new BatchPingTool();
            tool.setVisible(true);
        });
    }

    public BatchPingTool() {
        initUI();
        setTitle("BatchPing - 批量快速Ping工具");
        setSize(1100, 820);
        setMinimumSize(new Dimension(900, 650));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(createIconImage());
    }

    private Image createIconImage() {
        int size = 32;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(ACCENT_BLUE);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawOval(4, 4, size - 8, size - 8);
        g2.drawOval(10, 10, size - 20, size - 20);
        g2.drawOval(16, 16, size - 32, size - 32);
        g2.dispose();
        return img;
    }

    // ========================================
    //  UI初始化
    // ========================================
    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_DARK);
        setContentPane(root);

        root.add(createTitleBar(), BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(createInputPanel());
        splitPane.setRightComponent(createResultPanel());
        splitPane.setDividerLocation(400);
        splitPane.setDividerSize(4);
        splitPane.setBorder(null);
        splitPane.setBackground(BG_DARK);
        splitPane.setForeground(BORDER_SUBTLE);
        
        // 优化分隔条样式
        splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, 
            evt -> {
                splitPane.repaint();
            });
        root.add(splitPane, BorderLayout.CENTER);

        root.add(createStatusBar(), BorderLayout.SOUTH);
    }

    /**
     * 顶部标题栏
     */
    private JPanel createTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_CARD);
        bar.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_SUBTLE, 1),
            new EmptyBorder(14, 20, 14, 20)
        ));

        JLabel titleLabel = new JLabel("BatchPing");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(TEXT_PRIMARY);

        JLabel subtitleLabel = new JLabel("批量快速Ping工具  |  多线程并发探测  |  网段IP范围扫描  |  实时结果展示");
        subtitleLabel.setFont(FONT_SUBTITLE);
        subtitleLabel.setForeground(TEXT_MUTED);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);
        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        bar.add(textPanel, BorderLayout.WEST);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        addPresetBtn = new RoundedButton("预设", ACCENT_CYAN);
        addPresetBtn.addActionListener(e -> showPresetDialog());
        btnPanel.add(addPresetBtn);

        exportBtn = new RoundedButton("导出", TEXT_SECONDARY);
        exportBtn.addActionListener(e -> exportResults());
        btnPanel.add(exportBtn);

        bar.add(btnPanel, BorderLayout.EAST);
        return bar;
    }

    /**
     * 左侧面板：IP范围输入 + 主机列表 + 操作按钮
     */
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER_SUBTLE),
            new EmptyBorder(14, 16, 14, 16)
        ));

        // ---- 上部：IP范围输入 ----
        JPanel rangePanel = createIpRangePanel();
        panel.add(rangePanel, BorderLayout.NORTH);

        // ---- 中部：主机列表 ----
        JPanel hostPanel = new JPanel(new BorderLayout(0, 6));
        hostPanel.setOpaque(false);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        JLabel lbl = new JLabel("目标主机列表");
        lbl.setFont(FONT_SECTION);
        lbl.setForeground(TEXT_PRIMARY);
        headerRow.add(lbl, BorderLayout.WEST);
        JLabel hint = new JLabel("每行一个，支持 # 注释");
        hint.setFont(FONT_SUBTITLE);
        hint.setForeground(TEXT_MUTED);
        headerRow.add(hint, BorderLayout.EAST);
        hostPanel.add(headerRow, BorderLayout.NORTH);

        inputArea = new JTextArea();
        inputArea.setBackground(BG_INPUT);
        inputArea.setForeground(TEXT_PRIMARY);
        inputArea.setCaretColor(ACCENT_BLUE);
        inputArea.setFont(FONT_MONO);
        inputArea.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_SUBTLE, 1),
            new EmptyBorder(12, 14, 12, 14)
        ));
        inputArea.setLineWrap(false);
        inputArea.setTabSize(4);
        
        // 添加滚动条样式优化
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setBorder(null);
        inputScroll.getViewport().setBackground(BG_INPUT);
        inputScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        inputScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        // 自定义滚动条颜色
        inputScroll.getVerticalScrollBar().setUnitIncrement(16);
        inputScroll.getHorizontalScrollBar().setUnitIncrement(16);
        
        hostPanel.add(inputScroll, BorderLayout.CENTER);

        panel.add(hostPanel, BorderLayout.CENTER);

        // ---- 下部：操作按钮 ----
        JPanel btnRow = new JPanel(new GridLayout(1, 3, 10, 0));
        btnRow.setOpaque(false);
        btnRow.setBorder(new EmptyBorder(6, 0, 0, 0));

        startBtn = new RoundedButton("开始Ping", SUCCESS_GREEN);
        startBtn.addActionListener(e -> startPing());
        btnRow.add(startBtn);

        stopBtn = new RoundedButton("停止", FAIL_RED);
        stopBtn.setEnabled(false);
        stopBtn.addActionListener(e -> stopPing());
        btnRow.add(stopBtn);

        clearBtn = new RoundedButton("清空", WARN_AMBER);
        clearBtn.addActionListener(e -> clearAll());
        btnRow.add(clearBtn);

        panel.add(btnRow, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * IP 范围输入面板 --- 逐段输入框 _._._._ ~ _._._._
     */
    private JPanel createIpRangePanel() {
        JPanel outer = new JPanel(new BorderLayout(0, 8));
        outer.setOpaque(false);

        JLabel segTitle = new JLabel("IP 范围扫描");
        segTitle.setFont(FONT_SECTION);
        segTitle.setForeground(ACCENT_CYAN);
        outer.add(segTitle, BorderLayout.NORTH);

        // 卡片容器
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(BG_INPUT);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_SUBTLE, 1),
            new EmptyBorder(14, 16, 14, 16)
        ));

        // 中间区域：垂直排列三个部分（起始IP、终止IP、按钮）
        JPanel ipRowsPanel = new JPanel(new GridLayout(3, 1, 0, 8));
        ipRowsPanel.setOpaque(false);

        Dimension octetSize = new Dimension(56, 32);

        // ===== 第一行：起始IP =====
        JPanel startRow = new JPanel();
        startRow.setLayout(new BoxLayout(startRow, BoxLayout.X_AXIS));
        startRow.setOpaque(false);
        startRow.setAlignmentY(Component.CENTER_ALIGNMENT);

        JLabel startLabel = new JLabel("起始: ");
        startLabel.setFont(FONT_SUBTITLE);
        startLabel.setForeground(TEXT_SECONDARY);
        startLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        startRow.add(startLabel);

        startOctets = new OctetField[4];
        for (int i = 0; i < 4; i++) {
            if (i > 0) {
                startRow.add(createDotLabel());
            }
            startOctets[i] = new OctetField(octetSize, i);
            startRow.add(startOctets[i]);
        }

        // 设置默认值
        startOctets[0].setText("192");
        startOctets[1].setText("168");
        startOctets[2].setText("1");
        startOctets[3].setText("1");

        ipRowsPanel.add(startRow);

        // ===== 第二行：终止IP =====
        JPanel endRow = new JPanel();
        endRow.setLayout(new BoxLayout(endRow, BoxLayout.X_AXIS));
        endRow.setOpaque(false);
        endRow.setAlignmentY(Component.CENTER_ALIGNMENT);

        JLabel endLabel = new JLabel("终止: ");
        endLabel.setFont(FONT_SUBTITLE);
        endLabel.setForeground(TEXT_SECONDARY);
        endLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        endRow.add(endLabel);

        endOctets = new OctetField[4];
        for (int i = 0; i < 4; i++) {
            if (i > 0) {
                endRow.add(createDotLabel());
            }
            endOctets[i] = new OctetField(octetSize, i);
            endRow.add(endOctets[i]);
        }

        // 设置默认值
        endOctets[0].setText("192");
        endOctets[1].setText("168");
        endOctets[2].setText("1");
        endOctets[3].setText("254");

        ipRowsPanel.add(endRow);

        // ===== 第三行：操作按钮 =====
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setBorder(new EmptyBorder(4, 0, 0, 0));

        RoundedButton appendBtn = new RoundedButton("生成并追加", ACCENT_BLUE);
        appendBtn.addActionListener(e -> generateFromOctets(true));
        btnRow.add(appendBtn);

        RoundedButton replaceBtn = new RoundedButton("生成并替换", ACCENT_CYAN);
        replaceBtn.addActionListener(e -> generateFromOctets(false));
        btnRow.add(replaceBtn);

        ipRowsPanel.add(btnRow);

        card.add(ipRowsPanel, BorderLayout.CENTER);

        outer.add(card, BorderLayout.CENTER);
        return outer;
    }

    private JLabel createDotLabel() {
        JLabel dot = new JLabel(".");
        dot.setFont(FONT_OCTET);
        dot.setForeground(TEXT_MUTED);
        dot.setAlignmentY(Component.CENTER_ALIGNMENT);
        return dot;
    }

    /**
     * 单段IP输入框（0-255），固定大小，支持自动跳转
     */
    private class OctetField extends JTextField {
        private final int position; // 在IP中的位置(0-3)

        OctetField(Dimension size, int pos) {
            super();
            this.position = pos;
            
            // 设置尺寸 - 使用setPreferredSize确保固定大小
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
            
            setBackground(BG_DARK);
            setForeground(TEXT_PRIMARY);
            setCaretColor(ACCENT_BLUE);
            setFont(FONT_OCTET);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER_SUBTLE, 1),
                new EmptyBorder(5, 4, 5, 4)
            ));
            
            // 关键：禁用最大宽度限制，确保文本完整显示
            setColumns(3);
            setMargin(new java.awt.Insets(0, 2, 0, 2));
            
            // 添加焦点监听和键盘事件，实现自动跳转
            addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    selectAll(); // 获得焦点时全选文本
                }
            });
            
            addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyTyped(java.awt.event.KeyEvent e) {
                    char c = e.getKeyChar();
                    // 只允许数字
                    if (!Character.isDigit(c)) {
                        e.consume();
                    }
                    
                    // 如果当前已有3位数字，阻止继续输入
                    else if (getText().length() >= 3) {
                        e.consume();
                    }
                }
                
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    // 按右箭头或点号时跳转到下一段
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_RIGHT || 
                        e.getKeyChar() == '.') {
                        e.consume();
                        moveToNextField();
                    }
                    // 按左箭头时跳转到上一段
                    else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_LEFT) {
                        e.consume();
                        moveToPreviousField();
                    }
                    // 按退格键且文本为空时，跳转到上一段
                    else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_BACK_SPACE && 
                             getText().isEmpty()) {
                        e.consume();
                        moveToPreviousField();
                    }
                }
                
                @Override
                public void keyReleased(java.awt.event.KeyEvent e) {
                    // 输入3位数字后自动跳转到下一段
                    if (getText().length() == 3 && position < 3) {
                        SwingUtilities.invokeLater(() -> moveToNextField());
                    }
                }
            });
        }

        /**
         * 跳转到下一个输入框
         */
        private void moveToNextField() {
            OctetField[] fields = isStartField() ? startOctets : endOctets;
            if (position < 3) {
                fields[position + 1].requestFocusInWindow();
                fields[position + 1].selectAll();
            } else {
                // 如果是起始IP的最后一段，跳转到终止IP的第一段
                if (isStartField()) {
                    endOctets[0].requestFocusInWindow();
                    endOctets[0].selectAll();
                }
            }
        }

        /**
         * 跳转到上一个输入框
         */
        private void moveToPreviousField() {
            OctetField[] fields = isStartField() ? startOctets : endOctets;
            if (position > 0) {
                fields[position - 1].requestFocusInWindow();
                fields[position - 1].selectAll();
            } else {
                // 如果是终止IP的第一段，跳转到起始IP的最后一段
                if (!isStartField()) {
                    startOctets[3].requestFocusInWindow();
                    startOctets[3].selectAll();
                }
            }
        }

        /**
         * 判断是否是起始IP的字段
         */
        private boolean isStartField() {
            for (OctetField f : startOctets) {
                if (f == this) return true;
            }
            return false;
        }

        int getOctet() {
            try {
                int v = Integer.parseInt(getText().trim());
                return (v >= 0 && v <= 255) ? v : -1;
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        
        @Override
        public void setText(String text) {
            super.setText(text);
            // 验证输入范围
            try {
                int val = Integer.parseInt(text);
                if (val < 0 || val > 255) {
                    setForeground(FAIL_RED);
                } else {
                    setForeground(TEXT_PRIMARY);
                }
            } catch (NumberFormatException e) {
                setForeground(FAIL_RED);
            }
        }
    }

    /**
     * 从逐段输入框读取IP范围，生成IP列表
     * @param append true=追加, false=替换
     */
    private void generateFromOctets(boolean append) {
        // 读取起始IP
        int[] start = new int[4];
        for (int i = 0; i < 4; i++) {
            start[i] = startOctets[i].getOctet();
            if (start[i] < 0) {
                showToast("起始IP第" + (i + 1) + "段无效，请输入 0~255");
                return;
            }
        }
        // 读取终止IP
        int[] end = new int[4];
        for (int i = 0; i < 4; i++) {
            end[i] = endOctets[i].getOctet();
            if (end[i] < 0) {
                showToast("终止IP第" + (i + 1) + "段无效，请输入 0~255");
                return;
            }
        }

        String startIp = start[0] + "." + start[1] + "." + start[2] + "." + start[3];
        String endIp   = end[0]   + "." + end[1]   + "." + end[2]   + "." + end[3];

        long startLong = ipToLong(startIp);
        long endLong   = ipToLong(endIp);

        if (startLong > endLong) {
            showToast("起始IP不能大于终止IP: " + startIp + " > " + endIp);
            return;
        }

        long count = endLong - startLong + 1;
        if (count > 65535) {
            int result = JOptionPane.showConfirmDialog(this,
                "IP范围过大（" + count + " 个地址），ping探测可能耗时很长。\n是否继续？",
                "确认大范围扫描", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (result != JOptionPane.YES_OPTION) return;
        }
        if (count > 100000) {
            showToast("IP范围超过100000个，超出限制，请缩小范围");
            return;
        }

        // 生成IP列表
        StringBuilder sb = new StringBuilder();
        if (append && inputArea.getText().trim().length() > 0) {
            sb.append(inputArea.getText().trim()).append("\n");
        }
        for (long i = startLong; i <= endLong; i++) {
            sb.append(longToIp(i)).append("\n");
        }

        inputArea.setText(sb.toString().trim());
        String mode = append ? "追加" : "替换";
        statusLabel.setText("已" + mode + " " + count + " 个IP地址到主机列表 ("
            + startIp + " ~ " + endIp + ")");
        statusLabel.setForeground(ACCENT_CYAN);
    }

    /**
     * IP字符串转long
     */
    private long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) | Integer.parseInt(parts[i]);
        }
        return result & 0xFFFFFFFFL;
    }

    /**
     * long转IP字符串
     */
    private String longToIp(long value) {
        return ((value >> 24) & 0xFF) + "." +
               ((value >> 16) & 0xFF) + "." +
               ((value >> 8)  & 0xFF) + "." +
               (value & 0xFF);
    }

    // ========================================
    //  右侧结果面板
    // ========================================
    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel statsWrapper = createStatsPanel();
        panel.add(statsWrapper, BorderLayout.NORTH);

        progressBar = new GradientProgressBar();
        progressBar.setPreferredSize(new Dimension(0, 4));
        progressBar.setBackground(PROGRESS_BG);
        progressBar.setBorder(null);
        JPanel progressWrapper = new JPanel(new BorderLayout());
        progressWrapper.setOpaque(false);
        progressWrapper.setBorder(new EmptyBorder(2, 0, 6, 0));
        progressWrapper.add(progressBar, BorderLayout.CENTER);
        panel.add(progressWrapper, BorderLayout.CENTER);

        String[] columns = {"状态", "主机", "IP地址", "延迟", "TTL", "发送/接收", "丢包率", "完成时间"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        resultTable = new JTable(tableModel);
        styleTable(resultTable);
        
        // 优化表格滚动区域
        JScrollPane tableScroll = new JScrollPane(resultTable);
        tableScroll.setBorder(new LineBorder(BORDER_SUBTLE, 1));
        tableScroll.getViewport().setBackground(BG_DARK);
        tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScroll.getVerticalScrollBar().setUnitIncrement(16);
        tableScroll.getHorizontalScrollBar().setUnitIncrement(16);
        
        panel.add(tableScroll, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 5, 10, 0));
        panel.setOpaque(false);

        totalCountLabel  = makeStatWidget(panel, "目标总数", "0");
        successCountLabel= makeStatWidget(panel, "成功", "0");
        failCountLabel   = makeStatWidget(panel, "失败", "0");
        avgTimeLabel     = makeStatWidget(panel, "平均延迟", "--");
        elapsedLabel     = makeElapsedWidget(panel);

        return panel;
    }

    private JLabel makeStatWidget(JPanel parent, String title, String initialValue) {
        JPanel card = new JPanel(new BorderLayout(4, 3));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_SUBTLE, 1),
            new EmptyBorder(10, 14, 10, 14)
        ));
        JLabel tl = new JLabel(title);
        tl.setFont(FONT_STAT_LABEL);
        tl.setForeground(TEXT_MUTED);

        JLabel vl = new JLabel(initialValue);
        vl.setFont(FONT_STAT_NUM);
        vl.setForeground(TEXT_PRIMARY);

        card.add(tl, BorderLayout.NORTH);
        card.add(vl, BorderLayout.CENTER);
        parent.add(card);
        return vl;
    }

    private JLabel makeElapsedWidget(JPanel parent) {
        JPanel card = new JPanel(new BorderLayout(4, 3));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(ACCENT_CYAN.getRed(), ACCENT_CYAN.getGreen(), 
                ACCENT_CYAN.getBlue(), 80), 1),
            new EmptyBorder(10, 14, 10, 14)
        ));
        JLabel tl = new JLabel("已用时间");
        tl.setFont(FONT_STAT_LABEL);
        tl.setForeground(TEXT_MUTED);

        JLabel vl = new JLabel("00:00");
        vl.setFont(FONT_STAT_NUM);
        vl.setForeground(ACCENT_CYAN);

        card.add(tl, BorderLayout.NORTH);
        card.add(vl, BorderLayout.CENTER);
        parent.add(card);
        return vl;
    }

    // ========================================
    //  表格样式
    // ========================================
    private void styleTable(JTable table) {
        table.setBackground(BG_DARK);
        table.setForeground(TEXT_PRIMARY);
        table.setFont(FONT_TABLE);
        table.setRowHeight(34);
        table.setShowGrid(true);
        table.setGridColor(BORDER_SUBTLE);
        table.setSelectionBackground(new Color(82, 139, 255, 35));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        JTableHeader header = table.getTableHeader();
        header.setBackground(BG_CARD);
        header.setForeground(TEXT_SECONDARY);
        header.setFont(FONT_TABLE_HEADER);
        header.setBorder(new LineBorder(BORDER_SUBTLE, 1));
        header.setPreferredSize(new Dimension(0, 36));
        ((DefaultTableCellRenderer) header.getDefaultRenderer())
            .setHorizontalAlignment(SwingConstants.CENTER);

        int[] widths    = {70, 150, 120, 70, 60, 90, 70, 90};
        int[] maxWidths = {90, 9999, 9999, 90, 80, 110, 90, 110};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            if (maxWidths[i] < 9999) {
                table.getColumnModel().getColumn(i).setMaxWidth(maxWidths[i]);
            }
        }

        table.getColumnModel().getColumn(0).setCellRenderer(new StatusCellRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(new CenterRenderer());
        table.getColumnModel().getColumn(4).setCellRenderer(new CenterRenderer());
        table.getColumnModel().getColumn(5).setCellRenderer(new CenterRenderer());
        table.getColumnModel().getColumn(6).setCellRenderer(new CenterRenderer());
        table.getColumnModel().getColumn(7).setCellRenderer(new CenterRenderer());

        table.setDefaultRenderer(Object.class, new AlternateRowRenderer());
    }

    private class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(FONT_TABLE.deriveFont(Font.BOLD, 11f));

            String status = value != null ? value.toString() : "";
            switch (status) {
                case STATUS_SUCCESS -> { label.setForeground(SUCCESS_GREEN); label.setText("[OK]"); }
                case STATUS_FAIL    -> { label.setForeground(FAIL_RED);      label.setText("[NG]"); }
                case STATUS_TIMEOUT -> { label.setForeground(WARN_AMBER);    label.setText("[TO]"); }
                case STATUS_RUNNING -> { label.setForeground(ACCENT_BLUE);   label.setText("[..]"); }
                default             -> { label.setForeground(TEXT_MUTED);   label.setText("[--]"); }
            }

            if (!isSelected) {
                label.setBackground(row % 2 == 0 ? ROW_EVEN : ROW_ODD);
            }
            return label;
        }
    }

    private static class CenterRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            if (!isSelected) {
                label.setBackground(row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                label.setForeground(TEXT_PRIMARY);
            }
            return label;
        }
    }

    private class AlternateRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                label.setBackground(row % 2 == 0 ? ROW_EVEN : ROW_ODD);
                label.setForeground(TEXT_PRIMARY);
            }
            label.setBorder(new EmptyBorder(0, 6, 0, 6));
            return label;
        }
    }

    // ========================================
    //  状态栏
    // ========================================
    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_CARD);
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_SUBTLE),
            new EmptyBorder(6, 16, 6, 16)
        ));

        statusLabel = new JLabel("就绪 -- 点击「开始Ping」批量探测目标主机");
        statusLabel.setFont(FONT_SUBTITLE);
        statusLabel.setForeground(TEXT_SECONDARY);
        bar.add(statusLabel, BorderLayout.WEST);

        JLabel version = new JLabel("v2.2  |  huidoudour");
        version.setFont(FONT_SUBTITLE);
        version.setForeground(TEXT_MUTED);
        bar.add(version, BorderLayout.EAST);

        return bar;
    }

    // ========================================
    //  核心逻辑
    // ========================================

    private List<PingTarget> parseHosts() {
        List<PingTarget> list = new ArrayList<>();
        String text = inputArea.getText().trim();
        if (text.isEmpty()) return list;

        for (String line : text.split("\\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;

            int commentIdx = line.indexOf('#');
            if (commentIdx >= 0) {
                line = line.substring(0, commentIdx).trim();
            }
            if (line.isEmpty()) continue;

            list.add(new PingTarget(line));
        }
        return list;
    }

    private void startPing() {
        if (running) return;

        List<PingTarget> parsedTargets = parseHosts();
        if (parsedTargets.isEmpty()) {
            showToast("请至少输入一个目标主机地址");
            return;
        }

        targets.clear();
        targets.addAll(parsedTargets);
        resultMap.clear();
        rowToHostMap.clear();
        completedCount.set(0);
        successCount.set(0);
        failCount.set(0);
        synchronized (responseTimesLock) { responseTimes.clear(); }
        tableModel.setRowCount(0);

        for (int i = 0; i < targets.size(); i++) {
            PingTarget t = targets.get(i);
            PingResult r = new PingResult();
            r.host = t.host;
            r.status = "PENDING";
            r.responseTime = "--";
            r.ttl = "--";
            r.sent = "--";
            r.received = "--";
            r.loss = "--";
            r.timestamp = "--";
            resultMap.put(t.host, r);
            rowToHostMap.put(i, t.host);
            tableModel.addRow(new Object[]{
                STATUS_PENDING, r.host, "--", r.responseTime, r.ttl,
                r.sent + "/" + r.received, r.loss, r.timestamp
            });
        }

        updateStats();

        running = true;
        stopRequested = false;
        startBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        clearBtn.setEnabled(false);
        inputArea.setEnabled(false);
        setOctetFieldsEnabled(false);
        statusLabel.setText("正在批量Ping... (" + targets.size() + " 个目标)");
        statusLabel.setForeground(ACCENT_BLUE);
        progressBar.setProgress(0);
        startTime = System.currentTimeMillis();

        startElapsedTimer();

        int threadCount = Math.max(1, Math.min(targets.size(), 20));
        executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < targets.size(); i++) {
            final int index = i;
            final PingTarget target = targets.get(i);
            executor.submit(() -> pingHost(target, index));
        }

        new Thread(() -> {
            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.DAYS);
            } catch (InterruptedException ignored) {}

            SwingUtilities.invokeLater(() -> {
                running = false;
                shutdownTimer();
                startBtn.setEnabled(true);
                stopBtn.setEnabled(false);
                clearBtn.setEnabled(true);
                inputArea.setEnabled(true);
                setOctetFieldsEnabled(true);
                progressBar.setProgress(100);

                if (stopRequested) {
                    statusLabel.setText("已停止 -- 完成 " + completedCount.get()
                        + "/" + targets.size() + " 个探测");
                    statusLabel.setForeground(WARN_AMBER);
                } else {
                    int failed = failCount.get();
                    if (failed == 0) {
                        statusLabel.setText("全部完成 -- " + targets.size() + " 个目标全部可达");
                        statusLabel.setForeground(SUCCESS_GREEN);
                    } else if (failed == targets.size()) {
                        statusLabel.setText("全部失败 -- 请检查网络连接和目标地址");
                        statusLabel.setForeground(FAIL_RED);
                    } else {
                        statusLabel.setText("部分完成 -- " + successCount.get()
                            + " 成功, " + failed + " 失败");
                        statusLabel.setForeground(WARN_AMBER);
                    }
                }
                progressBar.setProgress(100);
            });
        }).start();
    }

    private void pingHost(PingTarget target, int rowIndex) {
        PingResult result = resultMap.get(target.host);
        if (result == null) return;

        result.status = "RUNNING";
        updateRowStatus(rowIndex, STATUS_RUNNING);

        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("ping", "-n", "4", "-w", "2000", target.host);
            } else {
                pb = new ProcessBuilder("ping", "-c", "4", "-W", "2", target.host);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            int exitCode = process.waitFor();

            String outputStr = output.toString();
            if (exitCode == 0) {
                parseSuccessResult(result, outputStr);
                successCount.incrementAndGet();
            } else {
                parseFailResult(result, outputStr);
                failCount.incrementAndGet();
            }
        } catch (IOException | InterruptedException e) {
            result.status = "FAIL";
            result.responseTime = "--";
            result.ttl = "--";
            result.sent = "--";
            result.received = "--";
            result.loss = "100%";
            failCount.incrementAndGet();
        }

        result.timestamp = timeFormat.format(new Date());
        completedCount.incrementAndGet();

        long elapsed = System.currentTimeMillis() - startTime;
        long seconds = elapsed / 1000;
        final String elapsedStr = String.format("%02d:%02d", seconds / 60, seconds % 60);

        SwingUtilities.invokeLater(() -> {
            String statusText = switch (result.status) {
                case "SUCCESS" -> STATUS_SUCCESS;
                case "TIMEOUT" -> STATUS_TIMEOUT;
                default        -> STATUS_FAIL;
            };
            tableModel.setValueAt(statusText, rowIndex, 0);
            tableModel.setValueAt(result.ip != null ? result.ip : "--", rowIndex, 2);
            tableModel.setValueAt(result.responseTime, rowIndex, 3);
            tableModel.setValueAt(result.ttl, rowIndex, 4);
            tableModel.setValueAt(
                (result.sent != null ? result.sent : "--") + "/" +
                (result.received != null ? result.received : "--"), rowIndex, 5);
            tableModel.setValueAt(result.loss, rowIndex, 6);
            tableModel.setValueAt(result.timestamp, rowIndex, 7);

            updateStats();
            elapsedLabel.setText(elapsedStr);
            int progress = !targets.isEmpty() ?
                completedCount.get() * 100 / targets.size() : 0;
            progressBar.setProgress(progress);
        });
    }

    private void updateRowStatus(int rowIndex, String statusText) {
        SwingUtilities.invokeLater(() -> {
            if (rowIndex < tableModel.getRowCount()) {
                tableModel.setValueAt(statusText, rowIndex, 0);
            }
        });
    }

    private void parseSuccessResult(PingResult result, String output) {
        result.status = "SUCCESS";

        java.util.regex.Matcher ipMatcher = java.util.regex.Pattern.compile(
            "(?:\\[|\\()(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(?:\\]|\\))"
        ).matcher(output);
        if (ipMatcher.find()) result.ip = ipMatcher.group(1);

        java.util.regex.Matcher ttlMatcher = java.util.regex.Pattern.compile(
            "(?i)ttl[=:]\\s*(\\d+)"
        ).matcher(output);
        result.ttl = ttlMatcher.find() ? ttlMatcher.group(1) : "--";

        java.util.regex.Matcher avgMatcher = java.util.regex.Pattern.compile(
            "(?i)Average\\s*=\\s*(\\d+)ms"
        ).matcher(output);
        if (avgMatcher.find()) {
            String avg = avgMatcher.group(1);
            result.responseTime = avg + "ms";
            try {
                synchronized (responseTimesLock) { responseTimes.add(Long.valueOf(avg)); }
            } catch (NumberFormatException ignored) {}
        } else {
            java.util.regex.Matcher timeMatcher = java.util.regex.Pattern.compile(
                "(?i)time[=<]\\s*(\\d+(\\.\\d+)?)\\s*ms"
            ).matcher(output);
            List<Long> times = new ArrayList<>();
            while (timeMatcher.find()) {
                try { times.add((long) Float.parseFloat(timeMatcher.group(1))); }
                catch (NumberFormatException ignored) {}
            }
            if (!times.isEmpty()) {
                long avg = 0;
                for (long t : times) avg += t;
                avg /= times.size();
                result.responseTime = avg + "ms";
                synchronized (responseTimesLock) { responseTimes.add(avg); }
            } else {
                result.responseTime = "<1ms";
            }
        }

        java.util.regex.Matcher statMatcher = java.util.regex.Pattern.compile(
            "(?i)Sent\\s*=\\s*(\\d+).*?Received\\s*=\\s*(\\d+).*?Lost\\s*=\\s*(\\d+)\\s*\\((\\d+)%"
        ).matcher(output);
        if (statMatcher.find()) {
            result.sent = statMatcher.group(1);
            result.received = statMatcher.group(2);
            result.loss = statMatcher.group(4) + "%";
        }
    }

    private void parseFailResult(PingResult result, String output) {
        if (output.contains("timed out") || output.contains("超时") ||
            output.contains("Request timed out")) {
            result.status = "TIMEOUT";
            result.responseTime = ">2000ms";
        } else if (output.contains("could not find host") || output.contains("找不到") ||
                   output.contains("Ping request could not find host")) {
            result.status = "FAIL";
            result.responseTime = "DNS错误";
        } else {
            result.status = "FAIL";
            result.responseTime = "--";
        }
        result.ttl = "--";
        result.ip = "--";
        result.sent = "4";
        result.received = "0";
        result.loss = "100%";
    }

    private void stopPing() {
        stopRequested = true;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        shutdownTimer();
        running = false;
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        clearBtn.setEnabled(true);
        inputArea.setEnabled(true);
        setOctetFieldsEnabled(true);
        statusLabel.setText("用户停止 -- 已完成 " + completedCount.get()
            + "/" + targets.size() + " 个探测");
        statusLabel.setForeground(WARN_AMBER);
    }

    private void clearAll() {
        if (running) return;
        inputArea.setText("");
        tableModel.setRowCount(0);
        targets.clear();
        resultMap.clear();
        rowToHostMap.clear();
        completedCount.set(0);
        successCount.set(0);
        failCount.set(0);
        synchronized (responseTimesLock) { responseTimes.clear(); }
        progressBar.setProgress(0);
        updateStats();
        elapsedLabel.setText("00:00");
        statusLabel.setText("就绪 -- 点击「开始Ping」批量探测目标主机");
        statusLabel.setForeground(TEXT_SECONDARY);
    }

    private void updateStats() {
        totalCountLabel.setText(String.valueOf(targets.size()));
        successCountLabel.setText(String.valueOf(successCount.get()));
        failCountLabel.setText(String.valueOf(failCount.get()));

        synchronized (responseTimesLock) {
            if (!responseTimes.isEmpty()) {
                long avg = 0;
                for (long t : responseTimes) avg += t;
                avg /= responseTimes.size();
                avgTimeLabel.setText(avg + "ms");
            } else {
                avgTimeLabel.setText("--");
            }
        }
    }

    private void startElapsedTimer() {
        timerExecutor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        timerExecutor.scheduleAtFixedRate(() -> {
            if (!running) {
                shutdownTimer();
                return;
            }
            long elapsed = System.currentTimeMillis() - startTime;
            long seconds = elapsed / 1000;
            final String str = String.format("%02d:%02d", seconds / 60, seconds % 60);
            SwingUtilities.invokeLater(() -> elapsedLabel.setText(str));
        }, 0, 200, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private void shutdownTimer() {
        if (timerExecutor != null && !timerExecutor.isShutdown()) {
            timerExecutor.shutdownNow();
        }
    }

    /**
     * 批量启用/禁用 IP 逐段输入框
     */
    private void setOctetFieldsEnabled(boolean enabled) {
        for (OctetField f : startOctets) f.setEnabled(enabled);
        for (OctetField f : endOctets)   f.setEnabled(enabled);
    }

    // ========================================
    //  预设对话框
    // ========================================
    private void showPresetDialog() {
        String[] presets = {
            "=== DNS服务器 ===",
            "8.8.8.8 # Google DNS",
            "8.8.4.4 # Google DNS备用",
            "1.1.1.1 # Cloudflare DNS",
            "1.0.0.1 # Cloudflare DNS备用",
            "114.114.114.114 # 国内通用DNS",
            "223.5.5.5 # 阿里DNS",
            "119.29.29.29 # 腾讯DNS",
            "180.76.76.76 # 百度DNS",
            "",
            "=== 常用网站 ===",
            "www.baidu.com",
            "www.taobao.com",
            "www.jd.com",
            "www.zhihu.com",
            "www.bilibili.com",
            "www.github.com",
            "www.google.com",
            "",
            "=== 内网常用 ===",
            "192.168.1.1 # 路由器",
            "192.168.1.100",
            "10.0.0.1",
            "172.16.0.1"
        };

        JDialog dialog = new JDialog(this, "选择预设目标", true);
        dialog.setSize(420, 520);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(BG_CARD);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(BG_CARD);
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel title = new JLabel("预设主机列表");
        title.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 15));
        title.setForeground(TEXT_PRIMARY);
        panel.add(title, BorderLayout.NORTH);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String p : presets) { listModel.addElement(p); }
        JList<String> list = new JList<>(listModel);
        list.setBackground(BG_INPUT);
        list.setForeground(TEXT_PRIMARY);
        list.setFont(new Font("Consolas", Font.PLAIN, 13));
        list.setSelectionBackground(new Color(82, 139, 255, 40));
        list.setSelectionForeground(TEXT_PRIMARY);
        list.setBorder(new LineBorder(BORDER_SUBTLE, 1));
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_INPUT);
        panel.add(scroll, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);

        JButton cancelBtn = new RoundedButton("取消", TEXT_MUTED);
        cancelBtn.addActionListener(e -> dialog.dispose());
        btnRow.add(cancelBtn);

        JButton appendBtn = new RoundedButton("追加到列表", ACCENT_BLUE);
        appendBtn.addActionListener(e -> {
            StringBuilder sb = new StringBuilder(inputArea.getText());
            for (String sel : list.getSelectedValuesList()) {
                if (sel.isEmpty() || sel.startsWith("===")) continue;
                if (sb.length() > 0 && !sb.toString().endsWith("\n")) sb.append("\n");
                sb.append(sel).append("\n");
            }
            inputArea.setText(sb.toString().trim());
            dialog.dispose();
        });
        btnRow.add(appendBtn);

        JButton replaceBtn = new RoundedButton("替换列表", ACCENT_CYAN);
        replaceBtn.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            for (String sel : list.getSelectedValuesList()) {
                if (sel.isEmpty() || sel.startsWith("===")) continue;
                sb.append(sel).append("\n");
            }
            if (sb.length() > 0) inputArea.setText(sb.toString().trim());
            dialog.dispose();
        });
        btnRow.add(replaceBtn);

        panel.add(btnRow, BorderLayout.SOUTH);
        dialog.add(panel);
        dialog.setVisible(true);
    }

    /**
     * 导出结果到CSV
     */
    private void exportResults() {
        if (tableModel.getRowCount() == 0) {
            showToast("没有可导出的数据");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("导出Ping结果");
        chooser.setSelectedFile(new File("ping_result_" +
            new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write('﻿');
                writer.write("状态,主机,IP地址,延迟,TTL,发送,接收,丢包率,完成时间");
                writer.newLine();
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        if (j > 0) writer.write(",");
                        String val = tableModel.getValueAt(i, j).toString();
                        if (j == 0) {
                            val = val.replace("[OK]", "").replace("[NG]", "")
                                     .replace("[TO]", "").replace("[..]", "")
                                     .replace("[--]", "").trim();
                        }
                        writer.write("\"" + val + "\"");
                    }
                    writer.newLine();
                }
                showToast("导出成功: " + file.getName());
            } catch (Exception e) {
                showToast("导出失败: " + e.getMessage());
            }
        }
    }

    private void showToast(String message) {
        JOptionPane.showMessageDialog(this, message, "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    // ========================================
    //  自定义组件
    // ========================================

    private static class RoundedButton extends JButton {
        private final Color accentColor;
        private boolean hovered = false;
        private boolean pressed = false;

        RoundedButton(String text, Color accent) {
            super(text);
            this.accentColor = accent;
            setFont(FONT_BUTTON);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setForeground(accent);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 20, 9, 20));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) { 
                    hovered = true; 
                    repaint(); 
                }
                @Override
                public void mouseExited(MouseEvent e) { 
                    hovered = false; 
                    pressed = false;
                    repaint(); 
                }
                @Override
                public void mousePressed(MouseEvent e) {
                    if (isEnabled()) {
                        pressed = true;
                        repaint();
                    }
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    pressed = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            // 背景色根据状态变化
            Color bgColor;
            if (!isEnabled()) {
                bgColor = new Color(accentColor.getRed(), accentColor.getGreen(),
                    accentColor.getBlue(), 10);
            } else if (pressed) {
                bgColor = new Color(accentColor.getRed(), accentColor.getGreen(),
                    accentColor.getBlue(), 45);
            } else if (hovered) {
                bgColor = new Color(accentColor.getRed(), accentColor.getGreen(),
                    accentColor.getBlue(), 30);
            } else {
                bgColor = new Color(accentColor.getRed(), accentColor.getGreen(),
                    accentColor.getBlue(), 15);
            }
            
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);

            // 边框
            float borderWidth = pressed ? 1.5f : 1.2f;
            int borderAlpha = isEnabled() ? (pressed ? 140 : 100) : 40;
            g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(),
                accentColor.getBlue(), borderAlpha));
            g2.setStroke(new BasicStroke(borderWidth));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
            g2.dispose();

            setForeground(isEnabled() ? accentColor : TEXT_MUTED);
            super.paintComponent(g);
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            setForeground(enabled ? accentColor : TEXT_MUTED);
            repaint();
        }
    }

    private static class GradientProgressBar extends JComponent {
        private int progress = 0;

        void setProgress(int p) {
            this.progress = Math.max(0, Math.min(100, p));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            g2.setColor(PROGRESS_BG);
            g2.fillRoundRect(0, 0, w - 1, h - 1, h / 2, h / 2);

            if (progress > 0) {
                int fillW = (w - 1) * progress / 100;
                Color startColor, endColor;
                if (progress < 50) {
                    startColor = ACCENT_BLUE; endColor = ACCENT_CYAN;
                } else if (progress < 100) {
                    startColor = ACCENT_CYAN; endColor = SUCCESS_GREEN;
                } else {
                    startColor = SUCCESS_GREEN; endColor = SUCCESS_GREEN;
                }
                g2.setPaint(new GradientPaint(0, 0, startColor, fillW, 0, endColor));
                g2.fillRoundRect(0, 0, fillW, h - 1, h / 2, h / 2);
            }
            g2.dispose();
        }
    }
}
