<meta charset="utf8">
<link href="css/style.css" rel="stylesheet">
<?php
    $conn=mysqli_connect("localhost:3305","root","");
    mysqli_select_db($conn,'db_admin');
    mysqli_query($conn,"set names utf8");
    $query="select * from tb_admin";
    $result=mysqli_query($conn,$query);
    echo "<table>";

    echo "<tr><th>姓名</th><th>密码</th><th>邮箱</th></tr>";
    $count=0;
    while ($arr=mysqli_fetch_assoc($result)) {
        $count++;
        $alt=($count % 2) ? "alt" : "";
        echo "<tr class={$alt}>
            <td>{$arr['姓名']}</td>
            <td>{$arr['密码']}</td>
            <td>{$arr['邮箱']}</td>
        </tr>";
    }
    echo "</table>";

    mysqli_free_result($result);
    mysqli_close($conn);
?>