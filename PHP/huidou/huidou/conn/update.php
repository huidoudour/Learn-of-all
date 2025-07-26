<meta charset="utf8">
<link href="css/style.css" rel="stylesheet">
<?php
    include 'conn.php';
    $result=mysqli_query($conn,"select * from tb_admin where id={$_GET['id']}");
    $arr=mysqli_fetch_assoc($result);
?>
<form method="post" action="update_ok.php">
    <table>
        <tr><td>姓名</td><td><input type="text" name="username" value="<?php echo $arr['姓名']?>"></td></tr>
        <tr><td>密码</td><td><input type="text" name="password" value="<?php echo $arr['密码']?>"></td></tr>
        <tr><td>邮箱</td><td><input type="text" name="email" value="<?php echo $arr['邮箱']?>"></td></tr>
        <tr><td><input type="hidden" name="id" value="<?php echo $arr['id']?>"></td>
            <td><input type="submit" name="submit" value="修改"></td></tr>
    </table>
</form>
<?php
    mysqli_free_result($result);
    mysqli_close($conn);
?>
