<?php
    class Result {
        private $shape;
        function __construct()
        {
            switch ($_POST['action']) {
                case'circle':
                    $this->shape=new Circle();
                    break;
                default:
                    $this->shape=false;
            }
        }
        function __toString()
        {
            if ($this->shape) {
                $result=$this->shape->shapeName.'的周长'.$this->shape->perimeter().'</br>';
                $result.=$this->shape->shapeName.'的面积'.$this->shape->area().'</br>';
                return $result;
            }else {
                return '没有这个形状';
            }

        }
    }
?>