<?php
    abstract class shape {
        public $shapeName;
        abstract function area();
        abstract function perimeter();
        protected function validate($value,$message="形状") {
            if ($value==""||! is_numeric($value)||$value<0) {
                echo '<font color="red">'.$message.'必须为非负值的数字，且不能为空</font><br>';
                return false;
            }else {
                return true;
            }
        }
    }
?>