<meta charset="utf8">
<?php
    echo "<h1>留言板</h1>";
    echo "<link href='css/style.css' rel='stylesheet'>";
    $conn=mysqli_connect("localhost:3305","root","");
    mysqli_select_db($conn,"jsei_msg");
    mysqli_query($conn,"set names utf8");
    $result=mysqli_query($conn,"select * from comment");
    echo "<table border='1'>";
    echo "<tr algin=center><th>id</th><th>姓名</th><th>日期</th><th>电子邮箱</th><th>IP地址</th><th>留言</th><th>回复</th></tr>";
    while($arr=mysqli_fetch_array($result)){
        echo "<tr>";
        echo "<td>".$arr['id']."</td>";
        echo "<td>".$arr['poster']."</td>";
        echo "<td>".$arr['date']."</td>";
        echo "<td>".$arr['mail']."</td>";
        echo "<td>".$arr['ip']."</td>";
        echo "<td>".$arr['comment']."</td>";
        echo "<td>".$arr['replay']."</td>";
        echo "</tr>";
    }
    echo "</table>";
?>