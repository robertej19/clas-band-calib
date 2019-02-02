package org.clas.fcmon.band;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.clas.fcmon.tools.FCApplication;
import org.jlab.detector.base.DetectorCollection;
import org.jlab.detector.base.DetectorDescriptor;
import org.jlab.detector.calib.utils.CalibrationConstants;
import org.jlab.detector.calib.utils.CalibrationConstantsListener;
import org.jlab.detector.calib.utils.CalibrationConstantsView;
//import org.root.basic.EmbeddedCanvas;
//import org.root.func.F1D;
//import org.root.histogram.H1D;
import org.jlab.groot.graphics.EmbeddedCanvas;
import org.jlab.groot.math.F1D;
import org.jlab.groot.data.H1F;
import org.jlab.groot.fitter.DataFitter;

public class BANDCalib_HV extends FCApplication implements CalibrationConstantsListener,ChangeListener {

    
    EmbeddedCanvas c = this.getCanvas(this.getName()); 
    CalibrationConstantsView      ccview = new CalibrationConstantsView();
    ArrayList<CalibrationConstants> list = new ArrayList<CalibrationConstants>();
    CalibrationConstants calib;
    JPanel                    engineView = new JPanel();

    int is1,is2;
    public DetectorCollection<F1D> adcFitL = new DetectorCollection<F1D>();
    public DetectorCollection<F1D> adcFitR = new DetectorCollection<F1D>();
    
    int runnumber = 0;
    double fitscale = 0.8;
    double x_fit_range = fitscale*BANDPixels.BANDPixels_x_axis_max;//This sets the fitting range based off the x axis range
    
    
    public BANDCalib_HV(String name, BANDPixels[] bandPix) {
        super(name,bandPix);    
     }

    public void init(int is1, int is2) {
    	
    	System.out.println("\tInitializing HV Calibration");
    	
        this.is1=is1;
        this.is2=is2;
        
        calib = new CalibrationConstants(3,
                "Left_Mean/F:Left_Sigma/F:Right_Mean/F:Right_Sigma/F");
        calib.setName("/calibration/band/gain_balance");
        calib.setPrecision(3);

       /* for (int i=0; i<3; i++) {
            
            int layer = i+1;
            //calib.addConstraint(3, EXPECTED_MIP_CHANNEL[i]-ALLOWED_MIP_DIFF, 
            //                       EXPECTED_MIP_CHANNEL[i]+ALLOWED_MIP_DIFF, 1, layer);
            // calib.addConstraint(calibration column, min value, max value,
            // col to check if constraint should apply, value of col if constraint should be applied);
            // (omit last two if applying to all rows)
            //calib.addConstraint(4, EXPECTED_MIP_CHANNEL[i]-ALLOWED_MIP_DIFF, 
            //                       EXPECTED_MIP_CHANNEL[i]+ALLOWED_MIP_DIFF, 1, layer);
        }*/
        
        list.add(calib);         
    }   
    public List<CalibrationConstants>  getCalibrationConstants(){
        return list;
    } 
    public JPanel getCalibPane() {        
        engineView.setLayout(new BorderLayout());
        JSplitPane enginePane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT); 
        ccview.getTabbedPane().addChangeListener(this);
        ccview.addConstants(this.getCalibrationConstants().get(0),this);

        enginePane.setTopComponent(c);
        enginePane.setBottomComponent(ccview);       
        enginePane.setResizeWeight(0.8);
        engineView.add(enginePane);
        return engineView;       
    }  

    public void analyze() {
    	
    	for( int layer = 0 ; layer<bandPix.length ; layer++) {							// loop over layer
    		
    		for (int sector=is1 ; sector<is2 ; sector++) {								// loop over sector in layer
    			for(int paddle=0; paddle<bandPix[layer].nstr[sector-1] ; paddle++) {	// loop over paddle in sector
    					
    					// Add entry for the unique paddle
    				int lidx = (layer+1);
    		        int pidx = (paddle+1);
    		        calib.addEntry(sector,lidx,pidx);
        			
    		        	// Fit both sides of paddle
    		        for( int lr = 1 ; lr < 3 ; lr++) {									// loop over left/right PMT in paddle
        				
        				//int x_fit_range = 7000;
    		        	//System.out.println("xrange is given as" + x_fit_range);
    		        	fit(layer, sector, paddle, lr, 0., 0.,x_fit_range);//x_fit_range);
        			}
    		        System.out.println("Done with Layer "+ lidx + ", Sector "+ sector + " , Component " + pidx);
            	} 
    		}        		
        }   	
    	
        calib.fireTableDataChanged();
    }
    
    public void fit(int layer, int sector, int paddle, int lr, double minRange, double maxRange, double x_fit_max){ 
        
        System.out.println("Running runnumber = "+runnumber);
        runnumber++;
        H1F h = bandPix[layer].strips.hmap2.get("H2_a_Hist").get(sector,lr,0).sliceY(paddle);
        if( h.getIntegral() < 50) {
     	   System.out.println("Integral value is " + h.getIntegral() + " so setting " + layer + " " + sector + " " + paddle + " to null");
     	   F1D f1 = null;
     	   if( lr == 1) adcFitL.add(layer, sector,paddle, f1);
     	   if( lr == 2) adcFitR.add(layer, sector,paddle, f1);
     	   return;
        	};
        
        //F1D  f1 = new F1D("f1","[amp]*gaus(x,[mean],[sigma])", 0,40000);
        /*F1D  f1 = new F1D("f1","[amp]*landau(x,[mean],[sigma])+[const]",500,x_fit_max);
        f1.setParameter(0, h.getMax() );
        f1.setParameter(1, h.getMean() );
        f1.setParameter(2, 1000 );
        f1.setParameter(3, 20 );
        DataFitter.fit(f1, h, "REQ");
        h.getFunction().show();
		   */
        	//**********************&&&&&&&&&&&&&&&&&&&&**********************&&&&&&&&&&&&&&&&&&&&**********************
        	//NOTE: The parameters below are chosen essentially randomly. They seem to work but are not optimized. They should be optimized. 
         //**********************&&&&&&&&&&&&&&&&&&&&**********************&&&&&&&&&&&&&&&&&&&&**********************
        	F1D f1 = new F1D("f1", "[amp]*landau(x,[mean],[sigma]) +[exp_amp]*exp([p]*x)", 0.0, x_fit_max);
         f1.setParameter(0, h.getMax()*0.8);
         f1.setParameter(1, h.getMean() );
         f1.setParameter(2, 1000 );
         f1.setParameter(3, h.getMax()*0.5 );
         f1.setParameter(4, -0.001);
         DataFitter.fit(f1, h, "REQ");
         h.getFunction().show();
 		//gmFunc.setParameter(0, maxCounts*0.8);
 		//gmFunc.setParLimits(0, maxCounts*0.5, maxCounts*1.2);
 		//gmFunc.setParameter(1, maxPos);
 		//gmFunc.setParameter(2, 200.0);
 		//gmFunc.setParLimits(2, 0.0,400.0);
 		//gmFunc.setParameter(3, maxCounts*0.5);
 		//gmFunc.setParameter(4, -0.001);
 		
         
		
		   


        double amp = h.getFunction().getParameter(0);
        double mean = h.getFunction().getParameter(1);
        double sigma = h.getFunction().getParameter(2);
        double offset = h.getFunction().getParameter(3);
        
        if( amp < 0 || sigma < 0 ) {
     	   if( lr == 1) adcFitL.add(layer, sector,paddle, null);
     	   if( lr == 2) adcFitR.add(layer, sector,paddle, null);
     	   return; 
        }
        
        int lidx = (layer+1);
        int pidx = (paddle+1);
        
        if( lr == 1) {
     	   adcFitL.add(layer, sector,paddle, f1);
     	   calib.setDoubleValue(mean, "Left_Mean", sector, lidx, pidx);
     	   calib.setDoubleValue(sigma, "Left_Sigma", sector, lidx, pidx);
        }
        if( lr == 2) {
     	   adcFitR.add(layer, sector,paddle, f1);
     	   calib.setDoubleValue(mean, "Right_Mean", sector, lidx, pidx);
     	   calib.setDoubleValue(sigma, "Right_Sigma", sector, lidx, pidx);

        }
     }

    
    /*public void updateCanvas(DetectorDescriptor dd) {
        
        this.getDetIndices(dd);   
        int  lr = dd.getOrder()+1;
        int ilm = ilmap;
        
        int nstr = bandPix[ilm].nstr[is-1];
        int min=0, max=nstr;
        
        c.clear(); c.divide(2, 4);
        c.setAxisFontSize(12);
        
//      canvas.setAxisTitleFontSize(12);
//      canvas.setTitleFontSize(14);
//      canvas.setStatBoxFontSize(10);
        
        H1F h;
        String alab;
        String otab[]={" L PMT "," R PMT "};
        String lab4[]={" ADC"," TDC"," OVERFLOW"};      
        String tit = "SEC "+is+" LAY "+(ilm+1);
       
        for(int iip=min;iip<max;iip++) {
            alab = tit+otab[lr-1]+(iip+1)+lab4[0];
            c.cd(iip-min);           
            
            // Draw one including overflow samples
            h = bandPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,lr,7).sliceY(iip); 
            h.setOptStat(Integer.parseInt("1000100")); 
            h.setTitleX(alab); h.setTitle(""); h.setFillColor(34); c.draw(h);
            
            // Draw one without overflow samples
            h = bandPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,lr,0).sliceY(iip); 
            h.setOptStat(Integer.parseInt("1000100")); 
            h.setTitleX(alab); h.setTitle(""); h.setFillColor(32); c.draw(h,"same");
            
            
            
            //h = bandPix[ilm].strips.hmap2.get("H2_a_Hist").get(is,0,0).sliceY(iip);
            //h.setFillColor(34); c.draw(h,"same");  
            
//            if (h.getEntries()>100) {h.fit(f1,"REQ");}
        }
        
        c.cd(max);
        alab = tit+otab[lr-1]+lab4[2];
        h = bandPix[ilm].strips.hmap1.get("H1_o_Hist").get(is, lr, 0);
        h.setOptStat(Integer.parseInt("1000100")); 
        h.setTitleX(alab); h.setTitle(""); h.setFillColor(32); c.draw(h);

       
        c.repaint();

    }
	*/
    

    public void updateCanvas(DetectorDescriptor dd) {
        
        this.getDetIndices(dd); 
        
        int lr = dd.getOrder()+1;
        int sector = dd.getSector();
        int component = dd.getComponent();   
        int layer = ilmap;
        //int ilm = ilmap;
               
        int nstr = bandPix[layer].nstr[is-1];
        int min=0, max=nstr;
        
        c.clear(); c.divide(2, 4);
        c.setAxisFontSize(12);

//      canvas.setAxisTitleFontSize(12);
//      canvas.setTitleFontSize(14);
//      canvas.setStatBoxFontSize(10);
        
             
        
        H1F h;
        String alab;
        String otab[]={" L PMT "," R PMT "};
        String calTitles[]={" ADC"," TDC"};
        //String lab4[]={" ADC"," TDC"," OVERFLOW"}; 
        String tit = "SEC "+sector+" LAY "+(layer+1);
        //String tit = "SEC "+is+" LAY "+(ilm+1);
        int bothFired = 0;
       
        // We will loop here for all the calibration plots we want to make for
        // selected pmt
        /*for(int iip=min;iip<max;iip++) {
            
        	alab = tit+otab[lr-1]+(iip+1)+calTitles[0];
            */
        	alab = tit+otab[0]+(component+1)+calTitles[0];
            c.cd(0);          
            // Pull the ADC histogram for 1st canvas plot
            h = bandPix[layer].strips.hmap2.get("H2_a_Hist").get(is,1,0).sliceY(component);
            // Draw one including overflow samples
            //h = bandPix[layer].strips.hmap2.get("H2_a_Hist").get(is,lr,7).sliceY(iip); 
            h.setOptStat(Integer.parseInt("1000100")); 
            h.setTitleX(alab); h.setTitle(""); h.setTitleY("Entries"); h.setFillColor(34); c.draw(h);
            
            // Draw one without overflow samples
            //h = bandPix[layer].strips.hmap2.get("H2_a_Hist").get(is,lr,0).sliceY(iip); 
            h = bandPix[layer].strips.hmap2.get("H2_a_Hist").get(is,1,7).sliceY(component);
            h.setOptStat(Integer.parseInt("1000100")); 
            h.setTitleX(alab); h.setTitle(""); h.setTitleY("Entries"); h.setFillColor(32); c.draw(h,"same");
            //} 
        
            
            F1D f1 = adcFitL.get(layer,is,component);
            F1D f2 = adcFitR.get(layer,is,component);
        	
            
            if( f1 != null && f2 != null) {
	            if( lr == 1) {
	            	f1.setLineColor(2);
	            	f2.setLineColor(3);	
	            }
	            if( lr == 2) {
	            	f1.setLineColor(1);
	            	f2.setLineColor(2);
	            }
	            f1.setLineWidth(2);
	            f2.setLineWidth(2);
            	c.draw(f1,"same");
            	c.draw(f2,"same");
        		bothFired = 1;
 
            }
            else if( lr == 1 && f1 != null) {
            	f1.setLineColor(2);
            	f1.setLineWidth(2);
            	c.draw(f1,"same");
            }
            else if( lr == 2 && f2 != null) {
            	f2.setLineColor(2);
            	f2.setLineWidth(2);
            	c.draw(f2,"same");
            }
            
	        c.cd(1);
        	alab = tit+otab[1]+(component+1)+calTitles[0];         
            // Pull the ADC histogram for 1st canvas plot
            h = bandPix[layer].strips.hmap2.get("H2_a_Hist").get(is,2,0).sliceY(component);
            // Draw one including overflow samples
            //h = bandPix[layer].strips.hmap2.get("H2_a_Hist").get(is,lr,7).sliceY(iip); 
            h.setOptStat(Integer.parseInt("1000100")); 
            h.setTitleX(alab); h.setTitle(""); h.setTitleY("Entries"); h.setFillColor(34); c.draw(h);
            
            // Draw one without overflow samples
            //h = bandPix[layer].strips.hmap2.get("H2_a_Hist").get(is,lr,0).sliceY(iip); 
            h = bandPix[layer].strips.hmap2.get("H2_a_Hist").get(is,2,7).sliceY(component);
            h.setOptStat(Integer.parseInt("1000100")); 
            h.setTitleX(alab); h.setTitle(""); h.setTitleY("Entries"); h.setFillColor(32); c.draw(h,"same");
            //}  
            
            F1D f3 = adcFitL.get(layer,is,component);
            F1D f4 = adcFitR.get(layer,is,component);
        	
            
            if( f3 != null && f4 != null) {
	            if( lr == 1) {
	            	f3.setLineColor(2);
	            	f4.setLineColor(3);	
	            }
	            if( lr == 2) {
	            	f3.setLineColor(1);
	            	f4.setLineColor(2);
	            }
	            f3.setLineWidth(2);
	            f4.setLineWidth(2);
            	c.draw(f3,"same");
            	c.draw(f4,"same");
        		bothFired = 1;
 
            }
            else if( lr == 1 && f3 != null) {
            	f1.setLineColor(2);
            	f1.setLineWidth(2);
            	c.draw(f3,"same");
            }
            else if( lr == 2 && f4 != null) {
            	f4.setLineColor(2);
            	f4.setLineWidth(2);
            	c.draw(f4,"same");
            }
            
               
            
            
        /*
        if( bothFired == 1) {
	        // For L-R time plot
	        canvas.cd(1);
	        H1F h2 = bandPix[layer].strips.hmap2.get("H2_t_Hist").get(is,0,0).sliceY(component);   
	        //H1F h2 = bandPix[layer].strips.hmap2.get("H2_t_Hist").get(is,component+1,2).projectionX();
		    h2.setTitleX(tit+" BAR "+(component+1)+" TL-TR (ns)"); h2.setTitleY("Entries"); h2.setFillColor(32);
		    canvas.draw(h2);            
	
		    // For L-R time plot with FADC time
		    canvas.cd(2);
		    h2 = bandPix[layer].strips.hmap2.get("H2_a_Hist").get(is,0,1).sliceY(component);   
		    h2.setTitleX(tit+" BAR "+(component+1)+" TL-TR (ns)"); h2.setTitleY("Entries"); h2.setFillColor(32);
		    canvas.draw(h2);  
		    
		    //  For L+R time plot iwht FADC time
		    canvas.cd(3);
		    H2F h3 = bandPix[layer].strips.hmap2.get("H2_a_Hist").get(sector, component, 8);
		    h3.setTitleX(tit+" BAR "+(component+1)+" TL+TR (ns) vs FADC Mean"); h3.setTitleY("TL+TR");
		    canvas.draw(h3); 
		    
		    // Add projection of above
		    canvas.cd(4);
		    h2 = h3.projectionY();
		    h2.setTitleX(tit+" BAR "+(component+1)+" TL+TR-TREF (ns)"); h2.setTitleY("Entries"); h2.setFillColor(32);
		    canvas.draw(h2);
        }
        */
        c.repaint();
        //End of plotting
    }
    
    
    
    
	@Override
	public void stateChanged(ChangeEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void constantsEvent(CalibrationConstants arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
		
	}
}
