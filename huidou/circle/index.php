<!DOCTYPE html>
<html>
    <head>
        <title>图形计算器（面向对象）</title>
        <meta charset="utf8">
    </head>
    <body>
        <h3>图形（面积&&周长）计算器</h3>

        <a href="index.php?action=circle">圆形</a>
        <hr>
        <?php
            spl_autoload_register(function($className) {
                require_once strtolower($className).".class.php";
            });
            echo new Form ("index.php");
            if (isset($_POST["sub"])) {
                echo new Result();
            }
        ?>
    </body>
</html>