    package com.oracle;

import java.awt.*;
import java.awt.event.*;
import java.util.Random;

import javax.swing.*;



public class Little_Game
{
	//TODO:变量“声明”在此
	
	int x=500;
	int y=500;
	int z=0;
	int a=500;
	int b=450;
	int c=400;
	int d=450;
	int e=450;
	int f=500;
	
	
	int fx=0;
	int dz=0;
	窗口	ck	= null;
	定时器	ds1	= null;

	Little_Game()
	{
		//TODO:程序初始化在此

		ck = new 窗口();
		//窗口 宽+2*立体边, 高+2*立体边+标题栏
		ck.setSize(5 * 2 + 1000, 700 + 2 * 5 + 25);
		//设定窗口可见性setVisible  true/false
		ck.setVisible(true);
		//延时的毫秒
		ds1 = new 定时器(50);
	}

	class 窗口 extends JFrame
	{
		菜单		cd		= null;
		面板		mb		= null;
		窗口监听器	exit	= null;
		键盘监听器	jp		= null;

		窗口()
		{
			jp = new 键盘监听器();
			this.addKeyListener(jp);

			cd = new 菜单();
			this.setJMenuBar(cd);

			mb = new 面板();
			this.add(mb);

			exit = new 窗口监听器();
			this.addWindowListener(exit);

			this.repaint();
		}

		class 菜单 extends JMenuBar
		{
			JMenu		dan;	//菜单
			JMenuItem	xiang1; //菜单项

			菜单监听器		cdjtq;

			菜单()
			{
				dan = new JMenu("游戏"); //菜单
				xiang1 = new JMenuItem("开局"); //菜单项

				this.add(dan);
				dan.add(xiang1);

				cdjtq = new 菜单监听器();
				xiang1.addActionListener(cdjtq);
			}

			class 菜单监听器 implements ActionListener
			{
				public void actionPerformed(ActionEvent e)
				{
					//TODO:菜单事件处理
					if (e.getSource() == xiang1)
					{

					}
				}
			}
		}

		class 窗口监听器 extends WindowAdapter
		{
			public void windowClosing(WindowEvent e)
			{
				ds1.xc.stop();
				System.exit(0);
			}
		}

		class 键盘监听器 implements KeyListener
		{
			//TODO:键盘处理，可加入KeyEvent.VK_XXX
			public void keyPressed(KeyEvent e)
			{
				switch (e.getKeyCode())
				{
				case KeyEvent.VK_LEFT://左
					x -=20;
					fx=2;
					if(x<0)
					{
						z +=20;
			
						x=0;
						if(z>0)
						{
							z=0;
						}
					}
					
					break;
				case KeyEvent.VK_RIGHT://右
					x +=20;
					fx=0;
					if(x>900)
					{
						z -=20;
						x=900;
						if(z<-2000)
						{
							z=-2000;
						}
					}
					break;
				case KeyEvent.VK_UP://上
					y -=20;
					fx=1;
					if(y<400)
					{
						y=400;
					}
					break;
				case KeyEvent.VK_DOWN://下
					
					y +=20;
					fx=3;
					if(y>550)
					{
						y=550;
					}
					break;
				}
				
				ck.repaint();
			}

			public void keyReleased(KeyEvent e)
			{
			}

			public void keyTyped(KeyEvent e)
			{
			}
		}

		class 面板 extends JPanel
		{
			鼠标监听器	sb	= null;

			面板()
			{
				sb = new 鼠标监听器();
				this.addMouseListener(sb);
				this.addMouseMotionListener(sb);
			}

			class 鼠标监听器 extends MouseAdapter implements MouseMotionListener
			{
				public void mousePressed(MouseEvent e)
				{
				}

				public void mouseDragged(MouseEvent e)
				{
				}

				public void mouseMoved(MouseEvent e)
				{
				}

				public void mouseReleased(MouseEvent e)
				{
				}

				public void mouseClicked(MouseEvent e)//鼠标单击
				{
					//mx my鼠标的位置
					int mx = e.getX();
					int my = e.getY();
					//鼠标左键	BUTTON1、右键BUTTON3
					if (e.getButton() == MouseEvent.BUTTON1)
					{
						//TODO:鼠标左键单击 
						x=mx;
						y=my;

					}

					repaint();
				}
			}

			public void paint(Graphics g)
			{
				//g.setColor(Color.BLUE);	//设定颜色：RED GREEN  BLUE  ORANGE  WHITE  GRAY BLACK PINK
				//g.drawRect(左,上,宽,高);	//矩形
				//g.fillRect(左,上,宽,高);	//实心矩形
				//g.drawOval(左,上,宽,高);	//椭圆
				//g.fillOval(左,上,宽,高);		//实心椭圆

				//Image  tu=(new ImageIcon( 路径 )).getImage();	//路径：图片-右键-属性-路径src/	
				//g.drawImage(tu,x,y,null);						

				//TODO:绘图在此
				Random random=new Random();
				int fx1=random.nextInt(4);
				int fx2=random.nextInt(4);
				int fx3=random.nextInt(4);
				switch (fx1)
				{
				case 0://右
					a +=20;
					if(a>2000)
					{
						a=2000;
					}
					break;
				case 2://左
					a -=20;
					if(a<0)
					{
						a=0;
					}
					break;
  				case 1://上
  					b -=20;
  					if(b<0)
					{
						b=0;
					}
  					break;
  				case 3://下
  					b +=20;
  					if(b>550)
					{
						b=550;
					}
  					break;
				}
				switch (fx2)
				{
				case 0://右
					c +=40;
					if(c>2000)
					{
						c=2000;
					}
					break;
				case 2://左
					c -=40;
					if(c<0)
					{
						c=0;
					}
					break;
  				case 1://上
  					d -=40;
  					if(d<0)
					{
						d=0;
					}
  					break;
  				case 3://下
  					d +=40;
  					if(d>550)
					{
						d=550;
					}
  					break;
				}
				switch (fx3)
				{
				case 0://右
					e +=60;
					if(e>2000)
					{
						e=2000;
					}
					break;
				case 2://左
					e -=60;
					if(e<0)
					{
						e=0;
					}
					break;
  				case 1://上
  					f -=60;
  					if(f<0)
					{
						f=0;
					}
  					break;
  				case 3://下
  					f +=60;
  					if(f>550)
					{
						f=550;
					}
  					break;
				}
				Image  tu=(new ImageIcon( "狐狸/5.png" )).getImage();	//路径：图片-右键-属性-路径src/	
				g.drawImage(tu,z,0,null);	
				Image  tu1=(new ImageIcon( "狐狸/"+fx+"-"+dz+".png" )).getImage();	//路径：图片-右键-属性-路径src/	
				g.drawImage(tu1,x,y,null);
				Image  tu2=(new ImageIcon( "狐狸/"+fx1+"-"+dz+".png" )).getImage();	//路径：图片-右键-属性-路径src/	
				g.drawImage(tu2,a,b,null);
				Image  tu3=(new ImageIcon( "狐狸/"+fx2+"-"+dz+".png" )).getImage();	//路径：图片-右键-属性-路径src/	
				g.drawImage(tu2,c,d,null);
				Image  tu4=(new ImageIcon( "狐狸/"+fx3+"-"+dz+".png" )).getImage();	//路径：图片-右键-属性-路径src/	
				g.drawImage(tu3,e,f,null);
				if(dz>=3)
				{
					dz=0;
				}
				else
				{
					dz++;
				}
				
				
				
				
				
				
				
				
				
				
				
			}
		}
	}

	class 定时器 implements Runnable//实现Runnable接口
	{
		Thread	xc	= null;
		long	jianGe;

		定时器(long jianGe)
		{
			this.jianGe = jianGe;
			xc = new Thread(this);
			xc.start();
		}

		public void run()
		{
			while (true)
			{
				try
				{
					xc.sleep(jianGe);
				
					if (this == ds1)
					{
						//TODO:定时处理
						switch (fx){
						case 2://左
							x -=20;
							if(x<0)
							{
								z +=20;
								x=0;
								if(z>0)
								{
									z=0;
								}
							}
							fx=2;
							break;
						case 0://右
							x +=20;
							if(x>900)
							{
								z -=20;
								x=900;
								if(z<-2000)
								{
									z=-2000;
								}
							}
							fx=0;
							break;
						case 1://上
							y -=20;
							if(y<400)
							{
								y=400;
							}
							fx=1;
							break;
						case 3://下
							y +=20;
							if(y>550)
							{
								y=550;
							}
							fx=3;
							break;
						}
						if(dz>=3)
						{
							dz=0;
						}
						else
						{
							dz++;
						}
						
						ck.repaint();
						
					}
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	//main主方法 ，主类的“入口方法”
	public static void main(String[] args)
	{
		new Little_Game();
	}
}