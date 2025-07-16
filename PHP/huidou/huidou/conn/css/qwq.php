<meta charset="utf8">
<link href="css/style.css" rel="stylesheet">
<?php
include 'conn.php';
if (isset($_POST['姓名'], $_POST['密码'], $_POST['邮箱'], $_POST['id'])) {
    $姓名 = $_POST['姓名'];
    $密码 = $_POST['密码'];
    $邮箱 = $_POST['邮箱'];
    $id = $_POST['id'];
    $query = "UPDATE tb_admin SET `姓名`='$姓名', `密码`='$密码', `邮箱`='$邮箱' WHERE `id`=$id";
    $result = mysqli_query($conn, $query);
    if ($result) {
        echo "<script>alert('修改成功');window.location.href='index.php'</script>";
    } else {
        echo "<script>alert('修改失败');window.location.href='index.php'</script>";
    }
} else {
    echo "<script>alert('缺少必要字段');window.location.href='index.php'</script>";
}
?>

<meta charset="utf8">
<link href="css/style.css" rel="stylesheet">
<?php
    include 'conn.php';
    $姓名=$_POST['姓名'];
    $密码=$_POST['密码'];
    $邮箱=$_POST['邮箱'];
    $id=$_POST['id'];
    $query="update tb_admin set '姓名'='{$姓名}','密码'='{$密码},'邮箱'='{$邮箱}' where 'id'={$id}";
    $result=mysqli_query($conn,$query);
    if ($result) {
        echo "<1script>alert('修改成功');window.location.href='index.php'</script>";
    } else {
        echo "<1script>alert('修改失败');window.location.href='index.php'</script>";
    }
?>