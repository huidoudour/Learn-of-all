<?php
    $conn=mysqli_connect("localhost:3305","root","");
    mysqli_select_db($conn,'db_admin');
    mysqli_query($conn,"set names utf8");
?>