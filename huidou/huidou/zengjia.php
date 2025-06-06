<meta charset="utf8">
<link href="css/style.css" rel="stylesheet">
<table>
    <form method="post">
        <tr><th colspan="2">管理员信息添加</th></tr>
        <tr><td>姓名</td><td><input type="text" name="姓名"></td></tr>
        <tr><td>密码</td><td><input type="password" name="密码"></td></tr>
        <tr><td>邮箱</td><td><input type="text" name="邮箱"></td></tr>
        <tr><td colspan="2"><input type="submit" name="submit" value="注册"></td></tr>
    </form>
</table>
<?php
    $conn=mysqli_connect("localhost:3305","root","") or die ("数据库连接失败".mysqli_connect_error());
    mysqli_select_db($conn,"db_admin") or die ("Error");
    mysqli_query($conn,"set names utf8");
    
    if (isset($_POST['submit'])) {
        $username = $_POST['姓名'];
        $password = $_POST['密码'];
        $email = $_POST['邮箱'];
        
        if (isset($username) && isset($password) && isset($email)) {
            $query = "INSERT INTO tb_admin (`姓名`, `密码`, `邮箱`) VALUES ('$username', '$password', '$email')";
            mysqli_query($conn, $query) or die ("执行SQL语句失败: " . mysqli_error($conn));
            echo "注册成功";
        } else {
            echo "数据填写不完整";
        }
    }
    mysqli_close($conn);
?>