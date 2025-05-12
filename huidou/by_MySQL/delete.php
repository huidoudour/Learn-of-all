<meta charset="utf8">
<?php
    include './conn.php';
    $id=$_GET['id'];
    $query="delete from tb_admin where id =[$id]";
    $result=mysqli_query($conn,$query) or die ("执行SQL语句失败");
    if ($result) {
        echo "<script>alert('删除成功');window.location.href='index.php'</script>";
    }
    else {
        echo "<script>alert('删除失败');window.location.href='index.php'</script>";
    }
?>