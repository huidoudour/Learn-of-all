<?php
    /**
     * 基础模型类model.class.php
     */
    class Model{
        protected $db;
        public function __construct(){
            $this->initDB();
        }
        private function initDB() {
            $dbConfig = array
            (
                'user' => 'root',
                'pass' => '',
                'dbname' => 'jsei_msg');
            $this->db= MYSQLPDO::getInstance($dbConfig);
    
        }
    }    
?>