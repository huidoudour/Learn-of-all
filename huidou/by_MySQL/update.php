<meta charset="utf8">
<link href="style.css" rel="stylesheet">
<!-- 
<?php
    include 'conn.php';
    
    $result=mysqli_query($conn,"select * from tb_admin where id={ $_GET ['id']}");
    $arr=mysqli_fetch_assoc($result);
?> 
-->
<?php
    $id = intval($_GET['id']); // 确保id是整数，防止SQL注入
    $result = mysqli_query($conn, "SELECT * FROM tb_admin WHERE id=$id");
    if(!$result) {
        die("查询失败: " . mysqli_error($conn)); // 添加错误处理
    }
?>
<form method="post" action="update_ok.php">
    <table>
        <tr><th colspan="2">管理员信息</th></tr>
        <tr><td>姓名</td><td><input type="text" name="username" value="<?php echo $arr['username']?>"></td></tr>
        <tr><td>密码</td><td><input type="text" name="password" value="<?php echo $arr['password']?>"></td></tr>
        <tr><td>邮箱</td><td><input type="text" name="email" value="<?php echo $arr['email']?>"></td></tr>
        <tr>
            <td><input type="hidden" name="id" value="<?php echo $arr['id']?>"></td>
            <td><input type="submit" name="submit" value="修改"></td>
        </tr>
    </table>
</frm>
<?php
    mysqli_free_result($result);
    mysqli_close($conn);
?>
