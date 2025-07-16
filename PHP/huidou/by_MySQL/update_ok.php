<meta charset="utf8">
<?php
    include 'conn.php';
    $username=$_POST['username'];
    $password=$_POST['password'];
    $email=$_POST['email'];
    $id=$_POST['id'];
    $query="update tb_admin set username='{$username}',password='$password',email='{$email}'where id ={$id}";
    $result=mysqli_query($conn,$query);
    if ($result) {
        echo "<script>alert('修改成功！');window.location.herf='index.php'</script>";
    }
    else {
        echo "<script>alert('修改失败!');windo.location.herf='index.php'</script>";
    }
?>