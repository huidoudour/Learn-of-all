<meta charset="utf8">
<link href="css/style.css" rel="stylesheet">
<?php
    include 'conn.php';
    $username=$_POST['username'];
    $password=$_POST['password'];
    $email=$_POST['email'];
    $id=$_POST['id'];
    $query="update tb_admin set user='{$姓名}',password='{$密码},email='{$邮箱}' where id={$id}";
    $result=mysqli_query($conn,$query);
    if ($result) {
        echo "<ecript>alert('修改成功');window.location.href='index.php'</script>";
    } else {
        echo "<ecript>alert('修改失败');window.location.href='index.php'</script>";
    }
?>