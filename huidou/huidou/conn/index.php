<meta charset="utf8">
<link href="css/style.css" rel="stylesheet">
<?php
    require './conn.php';
    $query="select * from tb_admin";
    $result=mysqli_query($conn,$query);
    echo "<table>";
    echo "<tr>
            <th>姓名</th>
            <th>密码</th>
            <th>邮箱</th>
            <th>编辑</th>
        </tr>";
    $count=0;
    while ($arr=mysqli_fetch_assoc($result)) {
        $count++;
        $alt=($count % 2) ? "alt" : "";
        $alt=($count % 2) ? "alt" : "";
        echo "<tr class={$alt}>
            <td>{$arr['姓名']}</td>
            <td>{$arr['密码']}</td>
            <td>{$arr['邮箱']}</td>";
        
        echo "<td><a href='update.php?id={$arr['id']}'>编辑</td>";
        echo "</tr>";
    }
    echo "</table>";
    mysqli_free_result($result);
    mysqli_close($conn);
?>