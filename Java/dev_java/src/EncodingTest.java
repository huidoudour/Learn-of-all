import java.io.*;
import java.nio.charset.StandardCharsets;

public class EncodingTest {
    public static void main(String[] args) {
        // 测试中文输出
        System.out.println("测试中文输出:");
        System.out.println("服务器启动成功！");
        System.out.println("内存设置: 2048M");
        System.out.println("状态: 运行中");
        
        // 测试错误输出
        System.err.println("测试错误信息:");
        System.err.println("读取服务器输出时出错");
        
        // 测试从控制台读取中文输入
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            System.out.print("请输入中文测试: ");
            String input = reader.readLine();
            System.out.println("你输入的是: " + input);
        } catch (IOException e) {
            System.err.println("读取输入时出错: " + e.getMessage());
        }
    }
}