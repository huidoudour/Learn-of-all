<?php
echo"欢迎来到PHP!OωO<br>";
echo "*****************************************<br>";
$r=10;
define("PI","3.1415");
$cicle = 2 * PI * $r;
$area = PI * $r * $r;
echo "圆的半径为".$r."<br>圆的周长为：".$cicle."<br>"."圆的面积为：".$area."<br>";
$output = "圆的半径为".$r."<br>";
$output = "圆的周长为".$cicle."<br>";
$output = "圆的面积为".$area."<br>";
echo $output;
echo "*****************************************<br>";
$h = 0;
$g = 1;
while ($g <= 100) {
    $h = $h + $g;
    $g = $g + 1;
}
echo "1+2+3+...+100=".$h;
echo "<br>*****************************************<br>";
for ($i = 0; $i < 6; $i++) {
    for ($j = $i; $j < 6; $j++) {
        echo " ";
    }
    for ($k = 0; $k < 2*$i-1; $k++) {
        echo "*";
    }
    echo "<br>";
}
?>