<?php

use Dom\Text;

    class Form {
        private $action;
        private $shape;
        function __construct($action="")
        {
            $this->action=$action;
            $this->shape=isset($_REQUEST["action"])?$_REQUEST["action"]:"rect";
        }
        function __toString()
        {
            $form='<form action="' . $this->action. '" method="post">';
            switch ($this->shape) {
                // case"rect":
                //     $form.=$this->getRect();
                //     break;
                // case"triangle":
                //     $form.=$this->getTriangle();
                //     break;
                case "circle":
                    $form.=$this->getCircle();
                    break;
                default:
                    $form.='请选择一个形状';
            }
            $form.='<input type="submit" name="sub" value="计算">';
            $form.='</form>';
            return $form;
        }
        private function getCircle() {
            $input='<b>请输入|圆形|的半径：</b>';
            $input.='半径：<input type="text" name="radius" value="'.(isset($_POST['radius'])?$_POST['radius']:'').'"><br>';
            $input.='<input type="hidden" name="action" value="circle">';
            return $input;
        }
    }
?>