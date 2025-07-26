<meta charset="utf8">
<link href="style.css" rel="stylesheet">
<?php
    require './conn.php';
    $query="select * from tb_admin";
    $result=mysqli_query($conn,$query) or die ("执行SQL语句失败");
    echo "<table>";
    echo "<tr><th>用户名</th><th>密码</th><th>邮箱</th><th>编辑</th><th>删除</th></tr>";
    $count=0;
    while ($arr=mysqli_fetch_assoc($result)) {
        $count++;
        $alt=($count % 2) ? "alt" : "";
        echo "<tr class={$alt}><td>{$arr['username']}</td><td>{$arr['password']}</td><td>{$arr['email']}</td>";
        echo "<td><a href='update.php?id={$arr['id']}'>编辑</a></td>";
        echo "<td><a href='delete.php?id={$arr['id']}'>删除</a></td>";
        echo "</tr>";
    }
    echo "</table>";
    mysqli_free_result($result);
    mysqli_close($conn);
?>