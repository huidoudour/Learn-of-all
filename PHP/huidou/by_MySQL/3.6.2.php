<meta charset="utf8">
<?php
    $conn=mysqli_connect("localhost","root","") or die ("Connect false".mysqli_connect_error());
    mysqli_select_db($conn,'db_admin') or die ("no this database".mysqli_error($conn));
    mysqli_query($conn,"set names utf8");
    $query="select * from tb_admin";
?>