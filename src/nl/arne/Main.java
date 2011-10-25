/*

 * BOXPOD
 * augmented reality drum loops
 * august 2009
 * 
 * by Arne Boon
 * exchange student at Kyushu University, Fukuoka, Japan
 * student of Interaction Design at Utrecht School of the Arts, Hilversum, the Netherlands
 * 
 * http://www.arneboon.nl/
 * arneboon@gmail.com
 * 
 */

package nl.arne;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.media.opengl.GL;
import javax.swing.Timer;

import jp.nyatla.nyar4psg.NyARBoard;
import processing.core.PApplet;
import processing.core.PFont;
import processing.opengl.PGraphicsOpenGL;
import processing.video.Capture;
import themidibus.MidiBus;

public class Main extends PApplet {
	
	//-------------------------------------------------------
	private static final long serialVersionUID = 1L;
	
	//font
	PFont font;
	boolean debugInfo = true;
	boolean debugMouseInput = true;
	int debugAxisNr = 4;
	
	//cam
	Capture cam;

	//midi
	MidiBus midiBus;
	int[] val = new int[4];
	char[] axis = {'x', 'y', 'z', 'p'};
	
	//maxinput
	boolean[] valMax = {false, false, false, false};
	int[] valZ = {0,0,0}; //old, new, difference
	
	//nya
	NyARBoard nya;
	float nyax = 0;
	float nyay = 0;
	final float so = 80;
	final float si = so*0.7f;
	
	//boxes grid
	int rows = 3;
	int cols = 8;
	float[][] boxH = new float[rows][cols];
	int sState = 0;
	int sRow = 0;
	int sCol = 0;
	
	//box
	final float boxPos = si/(cols+1);
	final float boxSpacing = si/(cols+1);
	final float boxRadius = si/20;
	final float boxHeightMax = 100;
	
	//sound
	Sample kick;
	Sample snare;
	Sample cowbell;
	ArrayList<Sample> kicks;
	ArrayList<Sample> snares;
	ArrayList<Sample> cowbells;
	
	//timer
	Timer timer;
	int phSpeed = 1000;
	int phPos = 0;
	
	//-------------------------------------------------------
	public Main() {
		//constructor
	}
	
	public static void main(String args[]) {
		PApplet.main(new String[] { "--present", "nl.arne.Main" });
	}
	
	//-------------------------------------------------------
	public void setup() {
		size(640, 480, OPENGL);
		frameRate(30);
		colorMode(HSB, 255);
		
		//cam
		println("loading cam...");
		cam = new Capture(this, 640, 480); //"USB2.0 Camera-WDM"
		//cam.settings();
		
		//midi
		println("loading midibus...");
//		MidiBus.list();
		midiBus = new MidiBus(this, 0, 3);
		
		//nya
		println("loading NyARBoard...");
		nya = new NyARBoard(this, width, height, "camera_para.dat", "marker.pat", 80);
		
		//boxes to zero
		setBoxHeightZero();
		
		//sounds
		println("loading samples...");
		kicks = new ArrayList<Sample>();
		snares = new ArrayList<Sample>();
		cowbells = new ArrayList<Sample>();
		
		for(int i = 0; i < cols; i++) {
			kicks.add(new Sample(this, "kick.wav", i));
			snares.add(new Sample(this, "snare.wav", i));
			cowbells.add(new Sample(this, "cowbell.wav", i));
		}
		
		//timer
		println("starting timer...");
		timer = new Timer((int) phSpeed, triggerSamples);
		timer.start();
		
		//font
		println("loading font...");
		font = createFont("Monospaced", 12, true);
		textFont(font);
		
		//val
		valZ[0] = val[2];
		println("SETUP COMPLETE");
	}
	
	//-------------------------------------------------------
	public void draw() {
		//cam
		if (cam.available() != true)
			return;
		cam.read(); 
		image(cam, 0, 0);
		
		//debug
		if(debugInfo)		
			debugInfo(13);
		if(debugMouseInput) {
			if(debugAxisNr > 0) { 
				val[debugAxisNr-1] = (int) map(mouseX, 0, width, 0, 127);
			}
		}
		
		//change sState by reading z-axis val[] difference
		//valRead: [0] old, [1] new, [2] difference
		valZ[1] = val[2];
		valZ[2] = Math.abs(valZ[0] - valZ[1]);
		if(valZ[2] > 15 && valMax[2] == false) {
			valMax[2] = true;
			if(sState < 2) {
				sState++;
			} else {
				sState = 0;
			}
		} else if (valZ[2] < 10) {
			valMax[2] = false;
		}
		valZ[0] = valZ[1];
		
		//selection state and input action
		switch(sState) {
			case 0:
				//-1 is extra row for playhead
				sRow = (int) Math.round(map(val[3], 0, 127, -1, rows-1));
				break;
			case 1:
				sCol = (int) Math.round(map(val[3], 0, 127, 0, cols-1));
				break;
			case 2:
				if(sRow >= 0 ) {
					boxH[sRow][sCol] = (float) map(val[3], 0, 127, 0, boxHeightMax);
				} else if (sRow == -1) {
					phSpeed = (int) map(val[3], 0, 127, 100, 1000);
					timer.setDelay(phSpeed);
				}
		}
		  
		//draw AR
		drawNya();
	}
	
	//-------------------------------------------------------
	public void setBoxHeightZero() {
		//fill boxHeight array
		for (int i = 0; i < rows; i++) {
		  for (int j = 0; j < cols; j++) {
		    boxH[i][j] = 0;
		  }
		}
		println("loading boxes...");
	}
	
	//-------------------------------------------------------
	ActionListener triggerSamples = new ActionListener() {
		public void actionPerformed(ActionEvent evt) {
			if(phPos < cols-1) {
				phPos++;
			} else {
				phPos = 0;
			}
			
			kicks.get(phPos).trigger(boxH[0][phPos]);
			snares.get(phPos).trigger(boxH[1][phPos]);
			cowbells.get(phPos).trigger(boxH[2][phPos]);
 		}
	};
	
	//-------------------------------------------------------
	public void drawNya() {
		if (nya.detect(cam)) {
			nyax = (nya.pos2d[0][0]+nya.pos2d[2][0])/2;
			nyay = (nya.pos2d[0][1]+nya.pos2d[2][1])/2;
			
			PGraphicsOpenGL pgl = (PGraphicsOpenGL) g;
			GL gl = pgl.beginGL();
			nya.beginTransform(pgl);
			gl.glClearDepth(0.0f);
			gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
			gl.glDepthFunc(GL.GL_GREATER);
			// begin AR ------------------
				//border
//				pushMatrix();
//					fill(100,0,0);
//					box(so,so,1);
//					fill(100,0,255);
//					box(si,si,2);
//				popMatrix();
				
				//boxes (translate, set color, create)
				pushMatrix();
					translate(-si / 2, -si/4, 0);
					
					for (int r = 0; r < rows; r++) {
						for (int c = 0; c < cols; c++) {
							switch(sState) {
								case 0:
									if(r == sRow) {
										fill(100,255,200);
										stroke(100,100,100, 100);
									} else {
										fill(100,100,10);
										stroke(100,0,100);
									}
									break;
								case 1:
									if(r == sRow && c == sCol) {
										fill(100,255,200);
										stroke(100,255,255);
									} else {
										fill(100,100,10);
										stroke(100,0,100);
									}
									break;
								case 2:
									if(r == sRow && c == sCol) {
										fill(100,100,10);
										stroke(100,255,255);
									} else {
										fill(100,100,10);
										stroke(100,0,100);
									}
									break;
							}
							
							//show phPos in columns
							if(c == phPos) {
								stroke(100, 255, 255);
							} else {
								stroke(100,0,100);
							}
							
							//boxes
							pushMatrix();
								translate(c*boxPos + boxSpacing, r*boxSpacing + boxSpacing*2, 0);
								pushMatrix();
									rotateZ(radians(45));
									rotateX(radians(90));
									
									drawCylinder(boxRadius, boxRadius, 0 + boxH[r][c], 4);
								popMatrix();
							popMatrix();
						}
					}
					
					//playhead (setColor) 
					switch(sState) {
						case 0:
							if(sRow == -1) {
								fill(100,255,200);
								stroke(100,255,255);
							} else {
								fill(100,100,10);
								stroke(100,0,100);
							}
							break;
						case 1:
							if(sRow == -1){
								fill(100,255,200);
								stroke(100,255,255);
							} else {
								fill(100,100,10);
								stroke(100,0,100);
							}
							break;
						case 2:
							if(sRow == -1) {
								fill(100,100,10);
								stroke(100,255,255);
							} else {
								fill(100,100,10);
								stroke(100,0,100);
							}
							break;
					}
					pushMatrix();
						translate(Math.round(phPos*boxPos) + boxSpacing, boxSpacing, 0);
						rotateZ(radians(45));
						rotateX(radians(90));
						drawCylinder(boxRadius, boxRadius, 1, 4);
					popMatrix();
				popMatrix();
				
				fill(0,0,0,0);
				stroke(0,0,0,0);
				noFill();
				noStroke();
				noSmooth();
	
			// end AR --------------------
			gl.glClearDepth(1.0f);
			gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
			pgl.endGL();
			nya.endTransform();
		} else {
			nyax = nyay = 0;
		}
	}

	//-------------------------------------------------------
	public void noteOn(int channel, int pitch, int velocity) {
	  // channel 0: X // channel 1: Y // channel 2: Z // channel 3: Pod
	  if(debugMouseInput == false)
		  val[channel] = velocity;  	
	}
	
	//-------------------------------------------------------
	public void drawCylinder(float topRadius, float bottomRadius, float tall, int sides) {
		float angle = 0;
		float angleIncrement = TWO_PI / sides;
		beginShape(QUAD_STRIP);
		for (int i = 0; i < sides + 1; ++i) {
			vertex(topRadius * cos(angle), 0, topRadius * sin(angle));
			vertex(bottomRadius * cos(angle), tall, bottomRadius * sin(angle));
			angle += angleIncrement;
		}
		endShape();
		noStroke();
		// If it is not a cone, draw the circular top cap
		if (topRadius != 0) {
			angle = 0;
			beginShape(TRIANGLE_FAN);
			// Center point
			vertex(0, 0, 0);
			for (int i = 0; i < sides + 1; i++) {
				vertex(topRadius * cos(angle), 0, topRadius * sin(angle));
				angle += angleIncrement;
			}
			endShape();
		}

		// If it is not a cone, draw the circular bottom cap
		if (bottomRadius != 0) {
			angle = 0;
			beginShape(TRIANGLE_FAN);
			// Center point
			vertex(0, tall, 0);
			for (int i = 0; i < sides + 1; i++) {
				vertex(bottomRadius * cos(angle), tall, bottomRadius
						* sin(angle));
				angle += angleIncrement;
			}
			endShape();
		}
	}
	
	//-------------------------------------------------------
	public void debugInfo(int h) {
		fill(170,255,200);
		text("nyax: " + Float.toString(nyax), 10, 2*h);
		text("nyab: " + Float.toString(nyay), 10, 3*h);
		text("debugMouseInput: " + Boolean.toString(debugMouseInput), 10, 4*h);
		
		for(int i = 0; i < axis.length; i++) {
			fill(170,255,200);
			text("midi "+axis[i]+": " + Integer.toString(val[i]), 10, 5*h+(h*i));
			fill(170,100,200);
			rect(90, 4.2f*h+(h*i), 127, h*0.9f);
			
			if(i < 3 && val[i] > 120) {
				fill(100,255,200);
			} else { 
				fill(170,255,200);
			}
			rect(90, 4.2f*h+(h*i), val[i], h*0.9f);
		}
		
		text("sState:" + Integer.toString(sState), 10, h*10);
		text("sRow:" + Integer.toString(sRow), 10, h*11);
		text("sCol:" + Integer.toString(sCol), 10, h*12);
		text("phSpeed:" + Integer.toString(phSpeed), 10, h*14);
		
	}
	
	//-------------------------------------------------------
	public void keyPressed()
	{
		switch(key) {
			case 'k':
				kicks.get(0).trigger(50);
				break;
			case 's':
				snares.get(7).trigger(50);
				break;
			case 'p':
				if(phPos < cols-1) {
					phPos++;
				} else {
					phPos = 0;
				}
				break;
			case 'r':
				setBoxHeightZero();
				break;
		}
	}
	
	//-------------------------------------------------------
	public void keyTyped() {
		switch(key) {
			case 'd':
				debugInfo = !debugInfo;
				println("debugState = " + debugInfo);
				break;
			case '.':
				if(debugAxisNr < 4) {
					debugMouseInput = true;
					debugAxisNr++;
				} else {
					debugMouseInput = false;
					debugAxisNr = 0;
				}
				break;
			case ',':
				if(debugAxisNr > 1) {
					debugMouseInput = true;
					debugAxisNr--;
				} 
				else if (debugAxisNr == 0) {
					debugMouseInput = true;
					debugAxisNr = 4;
				} 
				else {
					debugMouseInput = false;
					debugAxisNr = 0;
				}
				break;
			case 'm':
				debugMouseInput = false;
				debugAxisNr = 0;
				for(int i = 0; i < val.length; i++) {
					val[i] = 0;
				}
				break;
			case 'z':
				if(sState < 2) {
					sState++;
				} else {
					sState = 0;
				}
				break;
			default:
				println("key = " + key);			
		}
	}
	
	//-------------------------------------------------------
	public void stop() {
		super.stop();
	}
	

}
