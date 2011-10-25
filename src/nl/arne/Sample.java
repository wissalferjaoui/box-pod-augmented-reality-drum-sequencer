package nl.arne;

//import processing.core.PApplet;
import ddf.minim.AudioSample;
import ddf.minim.Minim;

public class Sample {
	
	Main p;
	String f;
	int i;
	
	Minim minim;
	AudioSample sample;
	
	//-------------------------------------------------------
	public Sample(Main parent, String fileName, int index) {
		p = parent;
		f = fileName;
		i = index;
		
		minim = new Minim(p);
		sample = minim.loadSample("sounds/"+f);
	}
	
	//-------------------------------------------------------
	public void trigger(float boxHeight) {
		if(boxHeight > 5) {
			sample.setGain(-20 + boxHeight/5);
//			PApplet.println(Float.toString(sample.getGain()));
			sample.trigger();
		}
	}
	
	//-------------------------------------------------------
	public void stop() {
		sample.close();
		minim.stop();
		p.stop();
	}
}
