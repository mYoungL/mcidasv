package edu.wisc.ssec.mcidasv.control.adt;

import java.lang.Math;
import java.lang.String;

@SuppressWarnings("unused")

public class ADT_Intensity {

   private static int MWAnalysisFlag;
   private static int Rule9StrengthFlag;

   private static double[] BDCurve_Points =
   { 30.0,  9.0,-30.0,-42.0,-54.0,
    -64.0,-70.0,-76.0,-80.0,-84.0,-100.0};

   public ADT_Intensity() {
      MWAnalysisFlag = 0;
      Rule9StrengthFlag = 0;
   }

   public static void CalculateIntensity(int RedoIntensityFlagValue,
                                         boolean RunFullAnalysis,
                                         String HistoryFileName)
   /**
    ** Compute intensity values CI, Final T#, and Raw T#.
    ** Inputs  : RedoIntensityFlag_Input  - Recalculate Intensity flag value
    ** Outputs : None
    ** Return  : 71 : storm is over land
    **            0 : o.k.
    */
   {
      int RetErr;
      double TnoRaw=0.0;
      double TnoFinal=0.0;
      double CI=0.0;
      double CIadjP=0.0;

      boolean InitStrengthTF = ADT_Env.InitStrengthTF;
      boolean LandFlagTF = ADT_Env.LandFlagTF;

      int RecLand = ADT_History.IRCurrentRecord.land;
      double Latitude = ADT_History.IRCurrentRecord.latitude;
      double Longitude = ADT_History.IRCurrentRecord.longitude;
      boolean LandCheckTF = true;

      if((LandFlagTF)&&(RecLand==1)) {
         /** Initialize Missing Record */
         /** throw exception ?? */
      } else {
         double RetVal[] = adt_TnoRaw(RedoIntensityFlagValue,HistoryFileName);
         TnoRaw = RetVal[0];
         MWAnalysisFlag = (int)RetVal[1];
         /** System.out.printf("RawT#Adj=%f  MWAnalysisFlag=%d\n",TnoRaw,MWAnalysisFlag); */
         ADT_History.IRCurrentRecord.Traw = TnoRaw;
      }

      /** System.out.printf("RunFullAnalysis=%b\n",RunFullAnalysis); */
      if(!RunFullAnalysis) {
         /** Perform Spot Analysis (only T#raw) */
         ADT_History.IRCurrentRecord.Tfinal = TnoRaw;
         ADT_History.IRCurrentRecord.CI = TnoRaw;
         CIadjP = adt_latbias(InitStrengthTF,LandFlagTF,HistoryFileName,TnoRaw,Latitude,Longitude);
         ADT_History.IRCurrentRecord.CIadjp = CIadjP;
         ADT_History.IRCurrentRecord.rule9 = 0;
      } else {
         int NumRecsHistory = ADT_History.HistoryNumberOfRecords();
         /** System.out.printf("numrecs=%d  inistrength=%b historyfilename=%s*\n",NumRecsHistory,InitStrengthTF,HistoryFileName); */
         /** System.out.printf("MWanalysisflag=%d\n",MWAnalysisFlag); */
         if((((NumRecsHistory==0)&&(InitStrengthTF))&&(HistoryFileName!=null))||(MWAnalysisFlag==1)) {
             System.out.printf("tnoraw=%f\n",TnoRaw);
            ADT_History.IRCurrentRecord.Tfinal = TnoRaw;
            ADT_History.IRCurrentRecord.CI = TnoRaw;
            ADT_History.IRCurrentRecord.CIadjp = 0.0;
            if(MWAnalysisFlag==1) {
               double[] RetVals = adt_CIno(HistoryFileName);
               CI = RetVals[0];
               Rule9StrengthFlag = (int)RetVals[1];
               ADT_History.IRCurrentRecord.CI = CI;
               ADT_History.IRCurrentRecord.rule9 = Rule9StrengthFlag;
               CIadjP = adt_latbias(InitStrengthTF,LandFlagTF,HistoryFileName,TnoRaw,Latitude,Longitude);
               ADT_History.IRCurrentRecord.CIadjp = CIadjP;
            } else {
               ADT_History.IRCurrentRecord.CI = TnoRaw;
               ADT_History.IRCurrentRecord.rule9 = 0;
            }
         } else {
            /** System.out.printf("rawt=%f\n",ADT_History.IRCurrentRecord.Traw); */
            TnoFinal = adt_TnoFinal(1);
            /** System.out.printf("FinalT#=%f\n",TnoFinal); */
            ADT_History.IRCurrentRecord.Tfinal = TnoFinal;
            double[] RetVals2 = adt_CIno(HistoryFileName);
            CI = RetVals2[0];
            Rule9StrengthFlag = (int)RetVals2[1];
            /** System.out.printf("CI#=%f Rule9StrengthFlag=%d \n",CI,Rule9StrengthFlag); */
            ADT_History.IRCurrentRecord.CI = CI;
            ADT_History.IRCurrentRecord.rule9 = Rule9StrengthFlag;
         }
      }

   }

   public static double[] adt_TnoRaw(int RedoIntensityFlag,String HistoryFileName)
   /**
    ** Compute initial Raw T-Number value using original Dvorak rules
    ** Inputs  : RedoIntensityFlag_Input - Redo Intensity Flag value
    **           MWAnalysisFlag_Input    - MW Analysis Flag value
    ** Outputs : None
    ** Return  : Raw T# value
    */
   {
      int RetErr;
      int XInc;
      int CloudBDCategory = 0;
      int EyeBDCategory = 0;
      int Rule8AdjCatValue = 0;
      int PreviousHistoryRecPtr = 0;
      double CloudTnoIntensity = -909.0;
      double TnoRawValue=0.0;
      double IntensityEstimateValue = 0.0;
      double TnoInterpAdjValue = 0.0;
      double FinalIntensityEstimateValue = 0.0;
      double ShearInterpAdjValue = 0.0;
      double CDOSizeRegressionAdjValue = 0.0;
      double EyeCloudTempDifferenceAdjValue = 0.0;
      double CDOSymmatryRegressionAdjValue = 0.0;
      double PreviousHistoryCIValue = 0.0;
      double CIValueAdjustmentFactorValue=0.0;
      double HistoryRecTime = 0.0;
      boolean First48HourShearTF = false;
      boolean MWOFFTF = false;
      boolean LandCheckTF = true;

      /** EYE SCENE REGRESSION BASE VALUES */
      double[][] EyeRegressionBaseArray = 
      /**                      DG   MG   LG   B    W    CMG  CDG           */
                  {{1.00,2.00,3.25,4.00,4.75,5.25,5.75,6.50,7.25,7.75,8.25},
                   {1.50,2.25,3.30,3.85,4.50,4.75,5.15,5.50,6.00,6.25,6.75}};
      /** CLOUD SCENE REGRESSION BASE VALUES */
      double[][] CloudRegressionBaseArray = 
      /**                      DG   MG   LG   B    W    CMG  CDG           */
                  {{2.00,2.40,3.25,3.50,3.75,4.00,4.10,4.20,4.30,4.40,4.70},
                   {2.05,2.40,3.00,3.20,3.40,3.55,3.65,3.75,3.80,3.90,4.10}};
      /** Curved Band Scene Type Intensity Values (each 20% of wrap around center) */
      double[] CurvedBandIntensityArray = {1.5,1.5,2.0,2.5,3.0,3.5,4.0};
      /** Shear Scene Type Distance Threshold Values (distance in km) */
      double[] ShearDistanceArray = {0.0,35.0,50.0,80.0,110.0,140.0};
      /** Shear Scene Type Intensity Values */
      double[] ShearIntensityArray = {3.50,3.00,2.50,2.25,2.00,1.50};

      /** Rule 8 Adjustments
       **  Row 1 - Shear Scenes (original rule 8)
       **  Row 2 - Eye Scenes (original + 0.5)
       **  Row 3 - Other Scenes (original - 0.5)
       */
      double[][] Rule8AdjArray = 
      /**                1hr  6hr 12hr 18hr 24hr                           */
                  {{0.0,0.51,1.01,1.71,2.21,2.71,0.0,0.0,0.21,0.51},
                   {0.0,0.51,1.01,2.71,3.21,3.71,1.31,0.0,0.21,0.51},
                   {0.0,0.51,0.71,1.21,1.71,2.21,0.0,0.0,0.21,0.51}};
      /** Eye Regression Factor (Atlantic, Pacific) */
      double[] EyeCloudTempDifferenceRegressionFactorArrayEYE = { 0.011, 0.015 };
      /** Cloud Region Symmatry Regression Factor - Eye Scene (Atlantic, Pacific) */
      double[] CloudSymmatryRegressionFactorArrayEYE = { -0.015, -0.015 };
      /** CDO size (km - Dark Gray BD Curve) - Cloud Scene (Atlantic, Pacific) */
      double[] CDOSizeRegressionFactorArrayCLD = { 0.002 , 0.001 };
      /** Cloud Region Symmatry Regression Factor - Cloud Scene (Atlantic, Pacific) */
      double[] CloudSymmatryRegressionFactorArrayCLD = { -0.030, -0.015 };

      double CurrentTime = 1900001.0;
      int ImageDate = 1900001;
      int ImageTime = 000000;;
      int RecDate = 1900001;
      int RecTime = 000000;
      int RecLand = 0;
      boolean LandFlagTF = ADT_Env.LandFlagTF;
      boolean InitStrengthTF = ADT_Env.InitStrengthTF;
      double InitStrengthValue = ADT_Env.InitRawTValue;
      int LandFlagCurrent = ADT_History.IRCurrentRecord.land;
      double RecTnoRaw = 0.0;

      int NumRecsHistory = ADT_History.HistoryNumberOfRecords();
      /** System.out.printf("numrecs=%d\n",NumRecsHistory); */
      if((NumRecsHistory==0)&&(HistoryFileName!=null)) {
         /**
          ** History file does not exist and current analysis is
          ** first analysis for the storm
          */
         if(InitStrengthTF) {
            ADT_History.IRCurrentRecord.TrawO = InitStrengthValue;
            return new double [] { InitStrengthValue, 0.0 };                      /* EXIT */
         }
      } else {
         double LastValidRecordTime=1900001.0;
         ImageDate = ADT_History.IRCurrentRecord.date;
         ImageTime = ADT_History.IRCurrentRecord.time;
         CurrentTime = ADT_Functions.calctime(ImageDate,ImageTime);
         /** System.out.printf("CurrentTime=%f\n",CurrentTime); */
         double FirstHistoryRecTime = LastValidRecordTime;
         if(NumRecsHistory!=0) {
            RecDate = ADT_History.HistoryFile[0].date;
            RecTime = ADT_History.HistoryFile[0].time;
            FirstHistoryRecTime = ADT_Functions.calctime(RecDate,RecTime);
            if((CurrentTime-FirstHistoryRecTime)<=2.0) First48HourShearTF = true;
         } else {
            FirstHistoryRecTime = LastValidRecordTime;
         }

         /** System.out.printf("FirstHistoryRecTime=%f\n",FirstHistoryRecTime); */
         XInc = 0;
         while(XInc<NumRecsHistory) {
            RecDate = ADT_History.HistoryFile[XInc].date;
            RecTime = ADT_History.HistoryFile[XInc].time;
            HistoryRecTime = ADT_Functions.calctime(RecDate,RecTime);
            /** System.out.printf("XInc= %d  HistoryRecTime%f\n",XInc,HistoryRecTime); */
            if(HistoryRecTime>CurrentTime) {
               break;
            }
            RecLand = ADT_History.HistoryFile[XInc].land;
            RecTnoRaw = ADT_History.HistoryFile[XInc].Traw;
            LandCheckTF = true;
            if(((LandFlagTF)&&(RecLand==1))||(RecTnoRaw<1.0)) {
               LandCheckTF = false;
            }
            if(LandCheckTF) {
               if((HistoryRecTime==CurrentTime)&&(XInc==0)&&(InitStrengthValue!=0.0)) {
                  /**
                   ** Current analysis is at or before first record in
                   ** existing history file - return global initial
                   ** strength value
                   */
                  System.out.printf("FIRST RECORD in NON-empty file\n");
                  ADT_History.IRCurrentRecord.TrawO = InitStrengthValue;
                  /* InitStrengthValue = 0.0; */
                  /* ADT_Env.InitRawTValue = 0.0; */
                  return new double[] {InitStrengthValue, 0.0 };  /* EXIT */
               }
               PreviousHistoryRecPtr = XInc;
               LastValidRecordTime = HistoryRecTime;
            }
            XInc++;
         }
         if(HistoryFileName!=null) {
            PreviousHistoryCIValue = ADT_History.HistoryFile[PreviousHistoryRecPtr].CI;
         } else {
            PreviousHistoryCIValue = 4.0;
         }

         /** System.out.printf("PreviousHistoryCIValue=%f\n",PreviousHistoryCIValue); */
         if(((CurrentTime-LastValidRecordTime)>1.0)&&
            (HistoryFileName!=null)) {
            /** The history file either begins with all land scenes
             ** or there is a break in the history file of greater than
             ** 24 hours.  Reinitialize the storm with the input
             ** Initial Classification value
             */
            ADT_History.IRCurrentRecord.TrawO = InitStrengthValue;
            ADT_Env.InitRawTValue = -1.0;
            /** System.out.printf("returning...\n"); */
            return new double[] { InitStrengthValue, 0.0 };  /* EXIT */
         }
      }

      double CloudTemperatureCurrent = ADT_History.IRCurrentRecord.cloudt;
      double EyeTemperatureCurrent = ADT_History.IRCurrentRecord.eyet;

      /** System.out.printf("current cloudT=%f  eyeT=%f\n",CloudTemperatureCurrent,EyeTemperatureCurrent); */
      for(XInc=0;XInc<10;XInc++) {
         /** compute cloud category */
         if((CloudTemperatureCurrent<=BDCurve_Points[XInc])&&
            (CloudTemperatureCurrent>BDCurve_Points[XInc+1])) {
            CloudBDCategory = XInc;
            CloudTnoIntensity = (CloudTemperatureCurrent-BDCurve_Points[CloudBDCategory])/
                                (BDCurve_Points[CloudBDCategory+1]-
                                 BDCurve_Points[CloudBDCategory]);
         }
         /** compute eye category for eye adjustment */
         if((EyeTemperatureCurrent<=BDCurve_Points[XInc])&&
            (EyeTemperatureCurrent>BDCurve_Points[XInc+1])) {
            EyeBDCategory = XInc;
         }
      }

      /** System.out.printf("cloudBD=%d  CloudTnoIntensity= %f eyeBD=%d  \n",CloudBDCategory,CloudTnoIntensity,EyeBDCategory); */
      int EyeSceneCurrent = ADT_History.IRCurrentRecord.eyescene;
      if(EyeSceneCurrent==1) {
         /** this matches DT used at NHC (jack beven) */
         /** EyeTemperature=(9.0+EyeTemperature)/2.0; */
         /** Eye Temp is between +9C (beven) and measured eye temp (turk) */
         EyeTemperatureCurrent = (EyeTemperatureCurrent+9.0)/2.0;
         ADT_History.IRCurrentRecord.eyet = EyeTemperatureCurrent;
      }

      /** category difference between eye and cloud region */
      double EyeCloudBDCategoryDifference = Math.max(0,CloudBDCategory-EyeBDCategory);
      /** System.out.printf("EyeCloudBDCategoryDifference=%f\n",EyeCloudBDCategoryDifference); */
    
      /** if scenetype is EYE */
      int CurvedBandBDAmountCurrent = ADT_History.IRCurrentRecord.ringcbval;
      int CurvedBandBDCategoryCurrent = ADT_History.IRCurrentRecord.ringcb;
      int CloudFFTValueCurrent = ADT_History.IRCurrentRecord.cloudfft;
      double MWScoreValueCurrent = ADT_History.IRCurrentRecord.mwscore;
      /** System.out.printf("MWScoreValueCurrent=%f\n",MWScoreValueCurrent); */

      MWAnalysisFlag = 0;
      if(RedoIntensityFlag==1) {
         ADT_Env.MWJulianDate = ADT_History.IRCurrentRecord.mwdate;
         ADT_Env.MWHHMMSSTime = ADT_History.IRCurrentRecord.mwtime;
         System.out.printf("****REDO : MW DATE=%d Time=%d\n",ADT_Env.MWJulianDate,ADT_Env.MWHHMMSSTime);
      }
      
      int CloudScene = ADT_History.IRCurrentRecord.cloudscene;
      int EyeScene = ADT_History.IRCurrentRecord.eyescene;
      int DomainID = ADT_Env.DomainID;
      double CloudSymAveCurrent = ADT_History.IRCurrentRecord.cloudsymave;
      double EyeCDOSizeCurrent = ADT_History.IRCurrentRecord.eyecdosize;
      /** System.out.printf("CloudScene=%d\n",CloudScene); */
      if(CloudScene==3) {
         /** CURVED BAND */
         CurvedBandBDAmountCurrent = Math.min(30,CurvedBandBDAmountCurrent+1);
         int CurvedBandBDPercentage = CurvedBandBDAmountCurrent/5;
         double IncrementMultFactor = 0.1;
         if(CurvedBandBDPercentage==1) {
            IncrementMultFactor = 0.2;
         }
         IntensityEstimateValue=CurvedBandIntensityArray[CurvedBandBDPercentage];
         TnoInterpAdjValue = IncrementMultFactor*
                             ((double)(CurvedBandBDAmountCurrent-(CurvedBandBDPercentage*5)));
         /**
          ** System.out.printf("CurvedBandBDAmount=%d  CurvedBandBDPercentage=%d CurvedBandBDCategory=%d  IntensityEstimateValue=%f TnoInterpAdjValue=%f\n",
          **         CurvedBandBDAmountCurrent,CurvedBandBDPercentage, CurvedBandBDCategoryCurrent,IntensityEstimateValue, TnoInterpAdjValue);
          */
         IntensityEstimateValue = IntensityEstimateValue+TnoInterpAdjValue;
         if(CurvedBandBDCategoryCurrent==5) {
            IntensityEstimateValue = Math.min(4.0,IntensityEstimateValue+0.5);
         }
         if(CurvedBandBDCategoryCurrent==6) {
            IntensityEstimateValue = Math.min(4.5,IntensityEstimateValue+1.0);
         }
         Rule8AdjCatValue=2;
      } else if(CloudScene==4) {
         /** POSSIBLE SHEAR -- new definition from NHC */
         XInc = 0;
         IntensityEstimateValue = 1.5;
         double ShearDistanceCurrent = ADT_History.IRCurrentRecord.eyecdosize;
         while(XInc<5) {
            if((ShearDistanceCurrent>=ShearDistanceArray[XInc])&&
               (ShearDistanceCurrent<ShearDistanceArray[XInc+1])) {
               ShearInterpAdjValue = (ShearDistanceCurrent-ShearDistanceArray[XInc])/
                                     (ShearDistanceArray[XInc+1]-
                                      ShearDistanceArray[XInc]);
               TnoInterpAdjValue = (ShearInterpAdjValue*
                                   (ShearIntensityArray[XInc+1]-
                                    ShearIntensityArray[XInc]));
               IntensityEstimateValue = ShearIntensityArray[XInc]+TnoInterpAdjValue;
               XInc=5;
            }
            else {
               XInc++;
            }
         }
         Rule8AdjCatValue = 0;
         if(First48HourShearTF) {
            IntensityEstimateValue=Math.min(2.5,IntensityEstimateValue);
            if(ADT_Env.DEBUG==100) {
               System.out.printf("Constraining SHEAR Intensity to 2.5 or less during first 48 hours\n");
            }
         }
      } else {
         /** EYE or NO EYE */
         /* int DomainID_Local = ADT_Env.DomainID = DomainID; */
         if(EyeScene<=2) {
            /** EYE */
            /**
             ** System.out.printf("EYE : EyeTemperature=%f  CloudTemperature=%f\n",
             **         EyeTemperatureCurrent,CloudTemperatureCurrent);
             ** System.out.printf("EYE : CloudTnoIntensity=%f  DomainID=%d CloudBDCategory=%d\n",CloudTnoIntensity,DomainID,
             **         CloudBDCategory);
             */
            TnoInterpAdjValue = (CloudTnoIntensity*
                    (EyeRegressionBaseArray[DomainID][CloudBDCategory+1]-
                     EyeRegressionBaseArray[DomainID][CloudBDCategory]));
            EyeCloudTempDifferenceAdjValue = 
                    EyeCloudTempDifferenceRegressionFactorArrayEYE[DomainID]*
                    (EyeTemperatureCurrent-CloudTemperatureCurrent);
            CDOSymmatryRegressionAdjValue = 
                    CloudSymmatryRegressionFactorArrayEYE[DomainID]*
                    (CloudSymAveCurrent);
            
            /** System.out.printf("EYE : cloudsymave=%f  CDOSymmatryRegressionAdjValue=%f\n",
                      CloudSymAveCurrent,CDOSymmatryRegressionAdjValue); */
             
            IntensityEstimateValue = 
                    EyeRegressionBaseArray[DomainID][CloudBDCategory]+
                    TnoInterpAdjValue+
                    EyeCloudTempDifferenceAdjValue+
                    CDOSymmatryRegressionAdjValue;
            
            /** System.out.printf("EYE :TnoInterpAdjValue=%f EyeCloudTempDifferenceAdjValue=%f CDOSymmatryRegressionAdjValue=%f IntensityEstimateValue=%f\n",
                      TnoInterpAdjValue,EyeCloudTempDifferenceAdjValue, CDOSymmatryRegressionAdjValue,IntensityEstimateValue); */
             
            IntensityEstimateValue = Math.min(IntensityEstimateValue,9.0);
            /** System.out.printf("IntensityEstimateValue=%f\n",IntensityEstimateValue); */
            if(EyeScene==2)  {
               /** LARGE EYE adjustment */
               IntensityEstimateValue = Math.min(IntensityEstimateValue-0.5,6.5);
            }
            Rule8AdjCatValue = 1;
         }
         else {
            /** NO EYE */
            /** CDO */
            TnoInterpAdjValue = (CloudTnoIntensity*
                    (CloudRegressionBaseArray[DomainID][CloudBDCategory+1]-
                     CloudRegressionBaseArray[DomainID][CloudBDCategory]));
            CDOSizeRegressionAdjValue = 
                    CDOSizeRegressionFactorArrayCLD[DomainID]*
                    EyeCDOSizeCurrent;
            
            /** System.out.printf("CDO : dgraysize=%f  CDOSymmatryRegressionAdjValue=%f\n",
                      EyeCDOSizeCurrent,CDOSizeRegressionAdjValue); */
             
            CDOSymmatryRegressionAdjValue = 
                    CloudSymmatryRegressionFactorArrayCLD[DomainID]*
                    (CloudSymAveCurrent);
            
            /* System.out.printf("CDO : cloudsymave=%f  CDOSymmatryRegressionAdjValue=%f\n",
                      CloudSymAveCurrent,CDOSymmatryRegressionAdjValue); */
             
            IntensityEstimateValue = 
                    CloudRegressionBaseArray[DomainID][CloudBDCategory]+
                    TnoInterpAdjValue+CDOSizeRegressionAdjValue+CDOSymmatryRegressionAdjValue;
            IntensityEstimateValue = IntensityEstimateValue-0.1; /** bias adjustment */
            /** CDO adjustment for very weak or very strong CDOs */
            if(CloudScene==0) {
               CIValueAdjustmentFactorValue=0.0;
               if(PreviousHistoryCIValue>=4.5) {
                  CIValueAdjustmentFactorValue = Math.max(0.0,Math.min(1.0,PreviousHistoryCIValue-4.5));
               }
               if(PreviousHistoryCIValue<=3.0) {
                  CIValueAdjustmentFactorValue = Math.min(0.0,Math.max(-1.0,PreviousHistoryCIValue-3.0));
               }
               IntensityEstimateValue = IntensityEstimateValue+CIValueAdjustmentFactorValue;
            }
            
            /** System.out.printf("CDO : TnoInterpAdjValue=%f CDOSizeRegressionAdjValue=%f CDOSymmatryRegressionAdjValue=%f IntensityEstimateValue=%f\n",
                      TnoInterpAdjValue,CDOSizeRegressionAdjValue,CDOSymmatryRegressionAdjValue,IntensityEstimateValue); */
             
            CIValueAdjustmentFactorValue = 0.0;
            /** EMBEDDED CENTER */
            if(CloudScene==1) {
               CIValueAdjustmentFactorValue = Math.max(0.0,Math.min(
                                            1.5,PreviousHistoryCIValue-4.0));
               
               /** System.out.printf("EMBC : PreviousHistoryCIValue=%f TnoInterpAdjValue=%f\n",
                         PreviousHistoryCIValue,CIValueAdjustmentFactorValue); */
                
               IntensityEstimateValue = IntensityEstimateValue+CIValueAdjustmentFactorValue;
            }
            /** IRREGULAR CDO (PT=3.5) */
            if(CloudScene==2) {
               /** additional IrrCDO bias adjustment */
               IntensityEstimateValue = IntensityEstimateValue+0.3;
               IntensityEstimateValue = Math.min(3.5,Math.max(2.5,IntensityEstimateValue));
            }
            Rule8AdjCatValue = 2;
         }
      }

      FinalIntensityEstimateValue = ((double)((int)((IntensityEstimateValue+0.01)*10.0)))/10.0;
      ADT_History.IRCurrentRecord.TrawO = FinalIntensityEstimateValue;
      
      /** System.out.printf("RawT#orig=%f\n",FinalIntensityEstimateValue); */
     
      /** NEW Microwave Eye Score logic */
      if(ADT_Env.DEBUG==100) {
         System.out.printf("***IntensityEstimateValue=%f\n",IntensityEstimateValue);
      }
    
      /** moved MW analysis here to work with all scenes */

      double[] RetVals2 = ADT_MWAdj.adt_calcmwadjustment(MWScoreValueCurrent,FinalIntensityEstimateValue);
      MWAnalysisFlag = (int)RetVals2[0];
      IntensityEstimateValue = RetVals2[1];
 
      if(ADT_Env.DEBUG==100) {
         System.out.printf("MWAnalysisFlag=%d  IntensityEstimateValue=%f\n", MWAnalysisFlag,IntensityEstimateValue);
      }
      FinalIntensityEstimateValue=(double)((int)((IntensityEstimateValue+0.01)*10.0))/10.0;
      /** System.out.printf("FinalIntensityEstimateValue=%f\n",FinalIntensityEstimateValue); */

      int Rule8Current = ADT_History.IRCurrentRecord.rule8;
      
      if(Rule8Current==34) MWOFFTF = true;
      
      boolean SpecialPostMWRule8EYEFlag = false;
      if((MWOFFTF)&&(Rule8AdjCatValue==1)) {
          /** System.out.printf("USING NEW POST-MW EYE RULE 8 CONSTRAINTS!!!\n"); */
          SpecialPostMWRule8EYEFlag = true;
      }

      /** System.out.printf("MWAnalysisFlag=%d\n",MWAnalysisFlag); */
      if(MWAnalysisFlag==0) {
         /**
          ** perform Dvorak EIR Rule 8 Constrants on Raw T# value
          ** "velden rule" (actually 86.4 minutes... 0.06 of a day)
          ** "additional velden rule", only over first 6 hours
          ** All cases     : delT of 0.5 over  1 hour  : rule8 = 9 "velden rule"
          **                 delT of 0.1 over  1 hour  : rule8 = 8 "additional"
          ** Raw T# <  4.0 : delT of 1.0 over  6 hours : rule8 = 2
          **                 No threshold exceeded     : rule8 = 0
          ** Raw T# >= 4.0 : delT of 1.0 over  6 hours : rule8 = 2
          **                 delT of 1.5 over 12 hours : rule8 = 3
          **                 delT of 2.0 over 18 hours : rule8 = 4
          **                 delT of 2.5 over 24 hours : rule8 = 5
          **                 No threshold exceeded     : rule8 = 0
          */
         double PreviousHistoryFinalTnoValue = FinalIntensityEstimateValue;
         int EyeSceneCounter = 0;
         int NonEyeSceneCounter = 0;
         double TnoValueMinus1hr = FinalIntensityEstimateValue;
         double TnoValueMinus6hr = FinalIntensityEstimateValue;
         double TnoValueMinus12hr = FinalIntensityEstimateValue;
         double TnoValueMinus18hr = FinalIntensityEstimateValue;
         double TnoValueMinus24hr = FinalIntensityEstimateValue;
         double RawTnoValueMinus6hrTime = CurrentTime;
         double RawTnoValueMinus6hr = FinalIntensityEstimateValue;
         double RawTnoValueMinus1hr = FinalIntensityEstimateValue;
         boolean First6hrRecordTF = false;
         if(NumRecsHistory!=0) {
            /**
             ** 0.0416 is one hour... round to 0.05 to make sure I catch the
             ** hour previous report
             */
            double CurrentTimeMinus1hr = CurrentTime-0.05;
            double CurrentTimeMinus6hr = CurrentTime-0.26;
            double CurrentTimeMinus12hr = CurrentTime-0.51;
            double CurrentTimeMinus18hr = CurrentTime-0.76;
            double CurrentTimeMinus24hr = CurrentTime-1.01;
            double TnoDifferenceValueMinus1hr;
            double TnoDifferenceValueMinus6hr;
            double TnoDifferenceValueMinus12hr;
            double TnoDifferenceValueMinus18hr;
            double TnoDifferenceValueMinus24hr;
            RecDate = ADT_History.HistoryFile[0].date;
            RecTime = ADT_History.HistoryFile[0].time;
            double FirstHistoryRecTime = ADT_Functions.calctime(RecDate,RecTime);
            double FirstHistoryLandRecTime = 1900001.0;
            boolean FirstHistoryLandRecTF = true;
            boolean CurrentTimeMinus24hrTF = false;
            boolean CurrentTimeMinus18hrTF = false;
            boolean CurrentTimeMinus12hrTF = false;
            boolean CurrentTimeMinus6hrTF = false;
            boolean CurrentTimeMinus1hrTF = false;
            boolean HistoryVeldenRuleFlagTF = false;
            boolean ApplyVeldenRuleTF = true;
            double RecTFinal;
            double RecTRaw;
            int RecRule9;
            int RecRapidDiss;
            int RecEyeScene;
            int RecCloudScene;
            int Rule8Val = 0;
            int PreviousHistoryRule9Value = 0;
            int PreviousHistoryRapidDissIDValue = 0;
            double Rule8TESTValue;

            if(FirstHistoryRecTime>=CurrentTimeMinus6hr) {
               First6hrRecordTF = true;
            }
            XInc = 0;
            while(XInc<NumRecsHistory) {
               RecDate = ADT_History.HistoryFile[XInc].date;
               RecTime = ADT_History.HistoryFile[XInc].time;
               RecLand = ADT_History.HistoryFile[XInc].land;
               HistoryRecTime = ADT_Functions.calctime(RecDate,RecTime);
               RecTFinal = ADT_History.HistoryFile[XInc].Tfinal;
               RecTRaw = ADT_History.HistoryFile[XInc].Traw;
               RecRule9 = ADT_History.HistoryFile[XInc].rule9;
               RecRapidDiss = ADT_History.HistoryFile[XInc].rapiddiss;
               RecEyeScene = ADT_History.HistoryFile[XInc].eyescene;
               RecCloudScene = ADT_History.HistoryFile[XInc].cloudscene;
               /** System.out.printf("currenttime=%f  historyrectime=%f\n",CurrentTime,HistoryRecTime); */
               if(HistoryRecTime>=CurrentTime) {
                  /** System.out.printf("outta here\n"); */
                  break;
               }
               LandCheckTF = true;
               /** System.out.printf("**RecLand=%d\n",RecLand); */
               if(((LandFlagTF)&&(RecLand==1))||(RecTnoRaw<1.0)) {
                  LandCheckTF = false;
                  if (FirstHistoryLandRecTF) {
                     FirstHistoryLandRecTime = HistoryRecTime;
                     FirstHistoryLandRecTF = false;
                  }
               } else {
                  FirstHistoryLandRecTF = true;
               }
               /** System.out.printf("**LandCheckTF=%b\n",LandCheckTF); */
               if((HistoryRecTime>=CurrentTimeMinus24hr)&&
                  (HistoryRecTime<CurrentTime)&&
                  (!CurrentTimeMinus24hrTF)&&(LandCheckTF)) {
                  CurrentTimeMinus24hrTF = true;
                  TnoValueMinus24hr = RecTFinal;
               }
               if((HistoryRecTime>=CurrentTimeMinus18hr)&&
                  (HistoryRecTime<CurrentTime)&&
                  (!CurrentTimeMinus18hrTF)&&(LandCheckTF)) {
                  CurrentTimeMinus18hrTF = true;
                  TnoValueMinus18hr = RecTFinal;
               }
               if((HistoryRecTime>=CurrentTimeMinus12hr)&&
                  (HistoryRecTime<CurrentTime)&&
                  (!CurrentTimeMinus12hrTF)&&(LandCheckTF)) {
                  CurrentTimeMinus12hrTF = true;
                  TnoValueMinus12hr = RecTFinal;
               }
               if((HistoryRecTime>=CurrentTimeMinus6hr)&&
                  (HistoryRecTime<CurrentTime)&&
                  (!CurrentTimeMinus6hrTF)&&(LandCheckTF)) {
                  CurrentTimeMinus6hrTF = true;
                  TnoValueMinus6hr = RecTFinal;
                  RawTnoValueMinus6hr = RecTRaw;
                  RawTnoValueMinus6hrTime = HistoryRecTime;
               }
               if((HistoryRecTime>=CurrentTimeMinus1hr)&&
                  (HistoryRecTime<CurrentTime)&&
                  (!CurrentTimeMinus1hrTF)&&(LandCheckTF)) {
                  CurrentTimeMinus1hrTF = true;
                  TnoValueMinus1hr = RecTFinal;
                  RawTnoValueMinus1hr = RecTRaw;
               }
               if((HistoryRecTime<CurrentTime)&&(LandCheckTF)) {
                  PreviousHistoryFinalTnoValue = RecTFinal;
                  PreviousHistoryRule9Value = RecRule9;
                  PreviousHistoryRapidDissIDValue = RecRapidDiss;
                  if(RecEyeScene<=2) {
                     EyeSceneCounter++;
                     NonEyeSceneCounter = 0;
                     if(EyeSceneCounter>=3||HistoryVeldenRuleFlagTF) {
                        ApplyVeldenRuleTF = false;
                        HistoryVeldenRuleFlagTF = true;
                     }
                  }
                  else {
                     EyeSceneCounter = 0;
                     NonEyeSceneCounter++;
                     if(NonEyeSceneCounter>=3) {
                        ApplyVeldenRuleTF = true;
                        HistoryVeldenRuleFlagTF = false;
                     }
                  }
                  if(PreviousHistoryRapidDissIDValue>=2) {
                     ApplyVeldenRuleTF = false;
                  }
               }
               XInc++;
            }
    
            /** System.out.printf("LandFlagCurrent=%d\n",LandFlagCurrent); */
            /* added to correctly analyze current record (08/27/13)*/
            if(LandFlagCurrent==2) {
               if(EyeScene<=2) {
                  if(EyeSceneCounter>=2||HistoryVeldenRuleFlagTF) {
                     ApplyVeldenRuleTF = false;
                  }
               }
               else {
                  ApplyVeldenRuleTF = true;
               }
            }
            /** System.out.printf("ApplyVeldenRuleTF=%b\n",ApplyVeldenRuleTF); */
            Rule8Val = (Rule8AdjCatValue*10)+0;
            /** System.out.printf("Rule8AdjCatValue=%d Rule8Val=%d\n",Rule8AdjCatValue,Rule8Val); */
            ADT_History.IRCurrentRecord.rule8 = Rule8Val;
            /** System.out.printf("PreviousHistoryFinalTnoValue=%f\n",PreviousHistoryFinalTnoValue); */
            /** System.out.printf("Rule8 value = %d\n",ADT_History.IRCurrentRecord.rule8); */
            if(PreviousHistoryFinalTnoValue<4.0) {
               /** System.out.printf(" LESS THAN 4.0\n"); */
               /* Raw T# < 4.0 */
               /** System.out.printf("First6hrRecordTF=%b\n",First6hrRecordTF); */
               if(First6hrRecordTF) {
                  if(CurrentTimeMinus1hrTF) {
                     TnoDifferenceValueMinus1hr = Math.abs(RawTnoValueMinus1hr-
                                              FinalIntensityEstimateValue);
                     if(TnoDifferenceValueMinus1hr>
                        Rule8AdjArray[Rule8AdjCatValue][8]) {
                        FinalIntensityEstimateValue = Math.max((RawTnoValueMinus1hr-
                                              Rule8AdjArray[Rule8AdjCatValue][8]),
                                              Math.min((RawTnoValueMinus1hr+
                                              Rule8AdjArray[Rule8AdjCatValue][8]),
                                              FinalIntensityEstimateValue));
                        Rule8Val = (Rule8AdjCatValue*10)+8;
                        ADT_History.IRCurrentRecord.rule8 = Rule8Val;
                     }
                  } else {
                     /*
                     **  no value available within past hour...
                     ** must determine approx value
                     */
                     TnoDifferenceValueMinus1hr = 0.1*(Math.abs(CurrentTime-RawTnoValueMinus6hrTime)/.0416);
                     double RawTnoMinimumValue = RawTnoValueMinus6hr - TnoDifferenceValueMinus1hr;
                     double RawTnoMaximumValue = RawTnoValueMinus6hr + TnoDifferenceValueMinus1hr;
                     if((FinalIntensityEstimateValue>RawTnoMaximumValue)||
                        (FinalIntensityEstimateValue<RawTnoMinimumValue)) {
                        FinalIntensityEstimateValue = Math.max(RawTnoMinimumValue,
                                              Math.min(RawTnoMaximumValue,
                                              FinalIntensityEstimateValue));
                        Rule8Val = (Rule8AdjCatValue*10)+8;
                        ADT_History.IRCurrentRecord.rule8 = Rule8Val;
                     }
                  }
               } else {
                  TnoDifferenceValueMinus1hr = Math.abs(TnoValueMinus1hr-
                                                   FinalIntensityEstimateValue);
                  /** System.out.printf("TnoDifferenceValueMinus1hr=%f\n",TnoDifferenceValueMinus1hr); */
                  if((TnoDifferenceValueMinus1hr>
                      Rule8AdjArray[Rule8AdjCatValue][9])&&
                     (CurrentTimeMinus1hrTF)&&(ApplyVeldenRuleTF)) {
                     FinalIntensityEstimateValue = Math.max(TnoValueMinus1hr-
                                              Rule8AdjArray[Rule8AdjCatValue][9],
                                              Math.min(TnoValueMinus1hr+
                                              Rule8AdjArray[Rule8AdjCatValue][9],
                                              FinalIntensityEstimateValue));
                     Rule8Val = (Rule8AdjCatValue*10)+9;
                     ADT_History.IRCurrentRecord.rule8 = Rule8Val;
                  }
                  TnoDifferenceValueMinus6hr = Math.abs(TnoValueMinus6hr-
                                             FinalIntensityEstimateValue);
                  /** System.out.printf("TnoDifferenceValueMinus6hr=%f\n",TnoDifferenceValueMinus6hr); */
                  /** System.out.printf("PreviousHistoryRule9Value=%d\n",PreviousHistoryRule9Value); */
                  if(PreviousHistoryRule9Value<2) {
                     if((TnoDifferenceValueMinus6hr>
                         Rule8AdjArray[Rule8AdjCatValue][2])&&
                        (CurrentTimeMinus6hrTF)) {
                        FinalIntensityEstimateValue = Math.max(TnoValueMinus6hr-
                                              Rule8AdjArray[Rule8AdjCatValue][2],
                                              Math.min(TnoValueMinus6hr+
                                              Rule8AdjArray[Rule8AdjCatValue][2],
                                              FinalIntensityEstimateValue));
                        Rule8Val = (Rule8AdjCatValue*10)+2;
                        ADT_History.IRCurrentRecord.rule8 = Rule8Val;
                     }
                  } else {
                     if((TnoDifferenceValueMinus6hr>
                         Rule8AdjArray[Rule8AdjCatValue][1])&&
                        (CurrentTimeMinus6hrTF)) {
                        FinalIntensityEstimateValue = Math.max(TnoValueMinus6hr-
                                              Rule8AdjArray[Rule8AdjCatValue][1],
                                              Math.min(TnoValueMinus6hr+
                                              Rule8AdjArray[Rule8AdjCatValue][1],
                                              FinalIntensityEstimateValue));
                        Rule8Val = (Rule8AdjCatValue*10)+1;
                        ADT_History.IRCurrentRecord.rule8 = Rule8Val;
                     }
                  }
               }
            } else {
               /** System.out.printf(" GREATER THAN 4.0\n"); */
               /* Raw T# >= 4.0 */
               TnoDifferenceValueMinus1hr = Math.abs(TnoValueMinus1hr-
                                                FinalIntensityEstimateValue);
               /** System.out.printf("TnoDifferenceValueMinus1hr=%f\n",TnoDifferenceValueMinus1hr); */
               if((TnoDifferenceValueMinus1hr>
                   Rule8AdjArray[Rule8AdjCatValue][9])&&
                  (CurrentTimeMinus1hrTF)&&(ApplyVeldenRuleTF)) {
                  FinalIntensityEstimateValue = Math.max(TnoValueMinus1hr-
                                              Rule8AdjArray[Rule8AdjCatValue][9],
                                              Math.min(TnoValueMinus1hr+
                                              Rule8AdjArray[Rule8AdjCatValue][9],
                                              FinalIntensityEstimateValue));
                  Rule8Val = (Rule8AdjCatValue*10)+9;
                  ADT_History.IRCurrentRecord.rule8 = Rule8Val;
               }
               TnoDifferenceValueMinus6hr = Math.abs(TnoValueMinus6hr-FinalIntensityEstimateValue);
               TnoDifferenceValueMinus12hr = Math.abs(TnoValueMinus12hr-FinalIntensityEstimateValue);
               TnoDifferenceValueMinus18hr = Math.abs(TnoValueMinus18hr-FinalIntensityEstimateValue);
               TnoDifferenceValueMinus24hr = Math.abs(TnoValueMinus24hr-FinalIntensityEstimateValue);
               /** System.out.printf("Rule8AdjCatValue=%d\n",Rule8AdjCatValue);
               /** System.out.printf("current6hrTF=%b TnoDifferenceValueMinus6hr=%f arrayval=%f \n",
                    CurrentTimeMinus6hrTF,TnoDifferenceValueMinus6hr,Rule8AdjArray[Rule8AdjCatValue][2]); */
               /** System.out.printf("current12hrTF=%b TnoDifferenceValueMinus12hr=%f arrayval=%f \n",
                    CurrentTimeMinus12hrTF,TnoDifferenceValueMinus12hr,Rule8AdjArray[Rule8AdjCatValue][3]); */
               /** System.out.printf("current18hrTF=%b TnoDifferenceValueMinus18hr=%f arrayval=%f \n",
                    CurrentTimeMinus18hrTF,TnoDifferenceValueMinus18hr,Rule8AdjArray[Rule8AdjCatValue][4]); */
               /** System.out.printf("current24hrTF=%b TnoDifferenceValueMinus24hr=%f arrayval=%f \n",
                    CurrentTimeMinus24hrTF,TnoDifferenceValueMinus24hr,Rule8AdjArray[Rule8AdjCatValue][5]); */

               /** NEW Rule 8 MW adjustment*/
               if(SpecialPostMWRule8EYEFlag) {
                  Rule8TESTValue=Rule8AdjArray[Rule8AdjCatValue][6];
               } else {
                  Rule8TESTValue=Rule8AdjArray[Rule8AdjCatValue][2];
               }

               if((TnoDifferenceValueMinus6hr>
                   Rule8TESTValue)&&
                  (CurrentTimeMinus6hrTF)) {
                  /** System.out.printf("6 hr\n"); */
                  FinalIntensityEstimateValue = Math.max(TnoValueMinus6hr-
                                              Rule8TESTValue,
                                              Math.min(TnoValueMinus6hr+
                                              Rule8TESTValue,
                                              FinalIntensityEstimateValue));
                  if(SpecialPostMWRule8EYEFlag) {
                     Rule8Val = (Rule8AdjCatValue*10)+6;
                  } else {
                     Rule8Val = (Rule8AdjCatValue*10)+2;
                  }
                  ADT_History.IRCurrentRecord.rule8 = Rule8Val;
               } else if((TnoDifferenceValueMinus12hr>
                        Rule8AdjArray[Rule8AdjCatValue][3])&&
                       (CurrentTimeMinus12hrTF)) {
                  /** System.out.printf("12 hr\n"); */
                  FinalIntensityEstimateValue = Math.max(TnoValueMinus12hr-
                                              Rule8AdjArray[Rule8AdjCatValue][3],
                                              Math.min(TnoValueMinus12hr+
                                              Rule8AdjArray[Rule8AdjCatValue][3],
                                              FinalIntensityEstimateValue));
                  Rule8Val = (Rule8AdjCatValue*10)+3;
                  ADT_History.IRCurrentRecord.rule8 = Rule8Val;
               } else if((TnoDifferenceValueMinus18hr>
                        Rule8AdjArray[Rule8AdjCatValue][4])&&
                       (CurrentTimeMinus18hrTF)) {
                  /** System.out.printf("18 hr\n"); */
                  FinalIntensityEstimateValue = Math.max(TnoValueMinus18hr-
                                              Rule8AdjArray[Rule8AdjCatValue][4],
                                              Math.min(TnoValueMinus18hr+
                                              Rule8AdjArray[Rule8AdjCatValue][4],
                                              FinalIntensityEstimateValue));
                  Rule8Val = (Rule8AdjCatValue*10)+4;
                  ADT_History.IRCurrentRecord.rule8 = Rule8Val;
               } else if((TnoDifferenceValueMinus24hr>
                        Rule8AdjArray[Rule8AdjCatValue][5])&&
                       (CurrentTimeMinus24hrTF)) {
                  /** System.out.printf("24 hr\n"); */
                  FinalIntensityEstimateValue = Math.max(TnoValueMinus24hr-
                                              Rule8AdjArray[Rule8AdjCatValue][5],
                                              Math.min(TnoValueMinus24hr+
                                              Rule8AdjArray[Rule8AdjCatValue][5],
                                              FinalIntensityEstimateValue));
                  Rule8Val = (Rule8AdjCatValue*10)+5;
                  ADT_History.IRCurrentRecord.rule8 = Rule8Val;
               } else {
                  /** System.out.printf("default \n"); */
                  ADT_History.IRCurrentRecord.rule8 = Rule8Val;
               }
            }
         }
      }
      /** System.out.printf("Rule 8 value @ end of intensity calc=%d\n",ADT_History.IRCurrentRecord.rule8); */
      if(MWOFFTF) {
         /* printf("holding Rule 8 flag to 34\n"); */
         ADT_History.IRCurrentRecord.rule8 = 34;
      }
      /*
      **  NOTE : additional function return points above
      **  - return Global Initial Strength Value if new history file or
      **    inserting before first record in existing file
      */

      if(ADT_Env.DEBUG==100) {
         System.out.printf("FinalIntensityEstimateValue=%f\n",FinalIntensityEstimateValue);
      }

      TnoRawValue = FinalIntensityEstimateValue;
      double ReturnMWAnalysisFlag = (double)MWAnalysisFlag;

      return new double[] { TnoRawValue, ReturnMWAnalysisFlag };
   }

   public static double adt_TnoFinal(int TimeAvgDurationID)
   /**
    ** Compute time averaged T-Number value using previous and current
    ** intensity estimates.  Average using a time-weighted averaging
    ** scheme.
    ** Inputs  : TimeAvgDurationID : time average duration flag : 0=6 hour;1=3 hour
    ** Outputs : None
    ** Return  : Final T# value
    */
   {
      /* double TnoFinalValue = 0.0; */

      double TnoFinalValue = ADT_History.IRCurrentRecord.Traw;
      
      /* int TimeAvgDurationID = 0; */
      double OneHourInterval = 1.0/24.0;
      double BaseTimeAvgValueHrs;
      double FinalTnoValue;
      double AverageValue = 0.0;
      double AverageValueSum = 0.0;
      double WeightValue = 0.0;
      double WeightValueSum = 0.0;
      boolean FoundValuesTF = false;
      boolean LandCheckTF = false;

      if(TimeAvgDurationID==1) {
         BaseTimeAvgValueHrs=3.0;   /* for NHC 3-hour time average value */
      }
      else if(TimeAvgDurationID==2) {
         BaseTimeAvgValueHrs=12.0;  /* for TIE Model time average value */
      }
      else {
         BaseTimeAvgValueHrs=6.0;
      }

      int ImageDate = ADT_History.IRCurrentRecord.date;
      int ImageTime = ADT_History.IRCurrentRecord.time;
      double CurrentTime = ADT_Functions.calctime(ImageDate,ImageTime);
      int NumRecsHistory = ADT_History.HistoryNumberOfRecords();
      boolean LandFlagTF = ADT_Env.LandFlagTF;

      /**
       ** compute average with current value with any values
       ** from previous 6 hours
       */
      double BeginningTime=CurrentTime-(BaseTimeAvgValueHrs/24.0);

      int XInc = 0;
      while(XInc<NumRecsHistory) {
         int RecDate = ADT_History.HistoryFile[XInc].date;
         int RecTime = ADT_History.HistoryFile[XInc].time;
         double HistoryRecTime = ADT_Functions.calctime(RecDate,RecTime);
         if((HistoryRecTime>=BeginningTime)&&(HistoryRecTime<CurrentTime)) {
            LandCheckTF = true;
            if(TimeAvgDurationID<=1) {
               AverageValue = ADT_History.HistoryFile[XInc].Traw;
            } else {
               if(AverageValue<0.0) {
                  AverageValue = 0.0;
               }
            }

            int RecLand = ADT_History.HistoryFile[XInc].land;
            if(((LandFlagTF)&&(RecLand==1))||(AverageValue<1.0)) {
               LandCheckTF = false;
            }
            if(LandCheckTF) {
               double TimeDifference=CurrentTime-HistoryRecTime;
               if(TimeAvgDurationID==0) {
                  /** time weighted average */
                  WeightValue = (BaseTimeAvgValueHrs-(TimeDifference/OneHourInterval));
               } else {
                  /** straight average */
                  WeightValue = BaseTimeAvgValueHrs;
               }
               AverageValueSum = AverageValueSum+(WeightValue*AverageValue);
               WeightValueSum = WeightValueSum+WeightValue;
               FoundValuesTF = true;
            }
         } else {
            if(FoundValuesTF) {
               break;
            }
         }
         XInc++;
      }
      /**
       ** compute time-averaged T# value.
       ** if no previous records found, return Raw T#
       */
      /** System.out.printf("TRAW=%f\n",ADT_History.IRCurrentRecord.Traw); */
      if(TimeAvgDurationID<=1) {
         AverageValue = ADT_History.IRCurrentRecord.Traw;
      } else {
         if(AverageValue<=1.0) {
            FoundValuesTF = false;
         }
      }
      /** System.out.printf("foundvaluestf=%b averagevalue=%f\n",FoundValuesTF,AverageValue); */
      if(FoundValuesTF) {
         AverageValueSum = AverageValueSum+(BaseTimeAvgValueHrs*AverageValue);
         WeightValueSum = WeightValueSum+BaseTimeAvgValueHrs;
         /** remove any value remainder past tenths */
         FinalTnoValue = (double)((int)(((AverageValueSum/WeightValueSum)+0.01)*10.0))/10.0;
      } else {
         FinalTnoValue = AverageValue;
      }
      /** System.out.printf("finaltnovalue=%f\n",FinalTnoValue); */

      TnoFinalValue = FinalTnoValue;

      return TnoFinalValue;
   }

   public static double[] adt_CIno(String HistoryFileName)
   /**
    ** Compute final CI-Number applying various Dvorak Rules, such
    ** as the now famous Rule 9
    ** Inputs  : None
    ** Outputs : CurrentStrenghIDValue_Return - current strengthening/weakening flag
    ** Return  : Current Intensity (CI) #
    */
   {
      int RetErr;
      int RapidDissIDValue = 0;
      int CurrentStrengthIDValue;
      int PreviousHistoryRule9Value = 0;;
      int Rule9IDValue;
      double PreviousHistoryFinalTnoValue;
      double IntensityValue;
      double TnoMinimumValue = 9.0;            /* T# minimum value */
      double TnoMaximumValue = 0.0;            /* T# maximum value */
      double Rule9AdditiveValue = 1.0;
      double CIadjP;
      boolean LandOnly12hrTF = true;           /* land only during last 12hrs logical*/
      boolean TDTSOnly24hrTF = false;          /* tropical dep/storm only last 24 hrs*/
      boolean LandCheckTF = true;

      int ImageDate = ADT_History.IRCurrentRecord.date;
      int ImageTime = ADT_History.IRCurrentRecord.time;
      double CurrentTime = ADT_Functions.calctime(ImageDate,ImageTime);
      double Latitude = ADT_History.IRCurrentRecord.latitude;
      double Longitude = ADT_History.IRCurrentRecord.longitude;
      boolean LandFlagTF = ADT_Env.LandFlagTF;
      double InitStrengthValue = ADT_Env.InitRawTValue;
      boolean InitStrengthTF = ADT_Env.InitStrengthTF;

      int NumRecsHistory = ADT_History.HistoryNumberOfRecords();
      if(NumRecsHistory==0) {
         /** no records in history file */
         IntensityValue = ADT_History.IRCurrentRecord.Traw;
         CurrentStrengthIDValue = 0;
         /** this will trip the RULE 9 FLAG for an initial classification of >=6.0 */
         if(InitStrengthValue>=6.0) {
            CurrentStrengthIDValue = 2;
         }
         /** Apply Latitude Bias Adjustment to CI value */
         CIadjP = adt_latbias(InitStrengthTF,LandFlagTF,HistoryFileName,IntensityValue,Latitude,Longitude);
         ADT_History.IRCurrentRecord.CIadjp = CIadjP;
         /** return Raw T# for CI# for initial analysis */
         return new double[] { IntensityValue, (double)CurrentStrengthIDValue };                                /* EXIT */
      }
      /** MW Eye Score Adjustment being applied... let CI# = Final T# and return */
      int Rule8Current = ADT_History.IRCurrentRecord.rule8;
      if((Rule8Current>=30)&&(Rule8Current<=33)) {
         IntensityValue = ADT_History.IRCurrentRecord.Tfinal;
         /** Apply Latitude Bias Adjustment to CI value */
         CIadjP = adt_latbias(InitStrengthTF,LandFlagTF,HistoryFileName,IntensityValue,Latitude,Longitude);
         ADT_History.IRCurrentRecord.CIadjp = CIadjP;
         CurrentStrengthIDValue = 0;
         return new double[] { IntensityValue, (double)CurrentStrengthIDValue };                                /* EXIT */
      }

      /** determine various time threshold values */
      double CurrentTimeMinus3Hrs = CurrentTime-0.125;
      double CurrentTimeMinus6Hrs = CurrentTime-0.25;
      double CurrentTimeMinus12Hrs = CurrentTime-0.5;
      double CurrentTimeMinus24Hrs = CurrentTime-1.0;
    
      /** find record just prior to current record */
      double PreviousHistoryTnoMaximumValue = 0.0;
      double PreviousHistoryTnoMaximum6hrValue = 0.0;
      double PreviousHistoryCIValue = 0.0;
      double PreviousHistoryRapidDissIDValue = 0;
      double PreviousHistoryRapidDissIDMinimumValue = 99;
      int XInc = 0;
      while(XInc<NumRecsHistory) {
         int RecDate = ADT_History.HistoryFile[XInc].date;
         int RecTime = ADT_History.HistoryFile[XInc].time;
         double HistoryRecTime = ADT_Functions.calctime(RecDate,RecTime);
         if(HistoryRecTime>=CurrentTime) {
            break;
         }
         int RecLand = ADT_History.HistoryFile[XInc].land;
         double Traw = ADT_History.HistoryFile[XInc].Traw;
         LandCheckTF = true;
         if(((LandFlagTF)&&(RecLand==1))||(Traw<1.0)) {
            LandCheckTF = false;
         }
         if(LandCheckTF) {
            PreviousHistoryFinalTnoValue = ADT_History.HistoryFile[XInc].Tfinal;
            PreviousHistoryCIValue = ADT_History.HistoryFile[XInc].CI;
            PreviousHistoryRule9Value = ADT_History.HistoryFile[XInc].rule9;
            PreviousHistoryRapidDissIDValue = ADT_History.HistoryFile[XInc].rapiddiss;
            /** check Rule 9 */
            if(HistoryRecTime>=CurrentTimeMinus6Hrs) {
               /** find largest finalT# in last 6 hours prior to current record */
               if(PreviousHistoryFinalTnoValue>PreviousHistoryTnoMaximumValue) {
                  PreviousHistoryTnoMaximumValue = PreviousHistoryFinalTnoValue;
               }
            }
            if(HistoryRecTime>=CurrentTimeMinus6Hrs) {
               /** if storm is over land for SIX hours, turn off Rule 9
                  (changed from 12 hours) */
               LandOnly12hrTF = false;
    
               /** rapid dissapation check */
               if(PreviousHistoryRapidDissIDValue<PreviousHistoryRapidDissIDMinimumValue) {
                  PreviousHistoryRapidDissIDMinimumValue = PreviousHistoryRapidDissIDValue;
               }
               if(PreviousHistoryFinalTnoValue>PreviousHistoryTnoMaximum6hrValue) {
                  PreviousHistoryTnoMaximum6hrValue = PreviousHistoryFinalTnoValue;
               }
            }
            if(HistoryRecTime>=CurrentTimeMinus24Hrs) {
               /** find min and max finalT# in last 24 hours prior to current record */
               if(PreviousHistoryFinalTnoValue<TnoMinimumValue) {
                  TnoMinimumValue = PreviousHistoryFinalTnoValue;
               }
               if(PreviousHistoryFinalTnoValue>TnoMaximumValue) {
                  TnoMaximumValue = PreviousHistoryFinalTnoValue;
               }
               /**
                ** if storm was TD/TS at any time during last 24 hours,
                ** don't allow trip of sig. strengthening flag
                */
               if(PreviousHistoryFinalTnoValue<=4.0) {
                  TDTSOnly24hrTF = true;
               }
            }
         }
         XInc++;
      }
    
      IntensityValue = ADT_History.IRCurrentRecord.Tfinal;
    
      if(XInc==0) {
         /** current record is before first record in history file */
         CurrentStrengthIDValue=0;
         /** Apply Latitude Bias Adjustment to CI value */
         CIadjP = adt_latbias(InitStrengthTF,LandFlagTF,HistoryFileName,IntensityValue,Latitude,Longitude);
         ADT_History.IRCurrentRecord.CIadjp = CIadjP;
         /** return Final T# for CI# for initial analysis */
         return new double[] { IntensityValue, (double)CurrentStrengthIDValue };                                /* EXIT */
      }
    
      Rule9IDValue = PreviousHistoryRule9Value;
      int[] ReturnValues3 = ADT_Functions.adt_oceanbasin(Latitude,Longitude);
      int BasinID_Local = ReturnValues3[0];
      
      /** rapid dissipation determination */
      double Slope6hrValue = ADT_Functions.adt_slopecal(6.0,2);
      if(PreviousHistoryRapidDissIDValue<=1) {
         RapidDissIDValue = 0;
         /** if(Slope6hrValue>=2.0) { */
         /* relax rapid weakening criteria for the East Pac */
         if(((BasinID_Local!=2)&&(Slope6hrValue>=2.0))||
            ((BasinID_Local==2)&&(Slope6hrValue>=1.5))) {
            /* 2.0/24 hours or 1.5/24 hours for East Pac */
            RapidDissIDValue = 1;
         }
         if((PreviousHistoryRapidDissIDMinimumValue==1)&&(RapidDissIDValue==1)) {
            Rule9AdditiveValue = 0.5;
            RapidDissIDValue = 2;
         }
      } else {
         Rule9AdditiveValue = 0.5;
         RapidDissIDValue = 2;
         /* if(Slope6hrValue<1.5) { */
         if(((BasinID_Local!=2)&&(Slope6hrValue<1.5))||
            ((BasinID_Local==2)&&(Slope6hrValue<1.0))) {
            /* 1.5/24 hours or 1.0/24 hours for East Pac */
            RapidDissIDValue = 3;
         }
         if((PreviousHistoryRapidDissIDMinimumValue==3)&&(RapidDissIDValue==3)) {
            Rule9AdditiveValue = 1.0;
            RapidDissIDValue = 0;
         }
      }
    
      /**
       ** strength flags : 0 - strengthening, but not significant
       **                  1 - applying Max's 12 hour max Tno. rule
       */
      /** determine CI# */
      double CurrentTnoValue = IntensityValue;
      double CurrentTnoPlusRule9Value = IntensityValue+Rule9AdditiveValue;
      /**
       ** We have once again returned to using the 6 hour Max T# value
       ** for all basins for the Rule 9 check
       */
      double CIValue = Math.min(CurrentTnoPlusRule9Value,
                    Math.max(PreviousHistoryTnoMaximumValue,CurrentTnoValue));

      /** will utilize Max's Rule all of the time */
      if(CIValue>CurrentTnoValue) {
         Rule9IDValue = 1;
      }
      if((PreviousHistoryRule9Value==1)&&(PreviousHistoryCIValue<=CurrentTnoValue)) {
         Rule9IDValue = 0;
      }
    
      /**
       ** check for land interaction
       ** if undergoing interactation with land "turn off" Rule 9
       ** application and return CI value to Adjusted Raw Tno value for >= 3 hours (was 12 hours)
       */
      if(LandOnly12hrTF) {
         /**
          ** if land flag is TRUE,
          ** turn off Rule 9 and let CI value be equal to the
          ** current Adjusted Raw T# value, and return (was Final T#)
          ** current intensity flag to "insignificant value" until
          ** another significant strengtheing cycle occurs
          */
         Rule9IDValue = 0;
         CIValue = ADT_History.IRCurrentRecord.Traw;
         RapidDissIDValue = 0;
      }
      /** Apply Latitude Bias Adjustment to CI value */
      CIadjP = adt_latbias(InitStrengthTF,LandFlagTF,HistoryFileName,CIValue,Latitude,Longitude);
      /** System.out.printf("CIno: ciadjp=%f\n",CIadjP); */
      ADT_History.IRCurrentRecord.CIadjp = CIadjP;
      ADT_History.IRCurrentRecord.rapiddiss = RapidDissIDValue;
    
      CurrentStrengthIDValue = Rule9IDValue;
    
      return new double[] { CIValue, (double)CurrentStrengthIDValue };
    
   }

   public static double adt_latbias(boolean InitStrengthTF,boolean LandFlagTF,
                                    String HistoryFileName,double InitialCIValue,
                                    double InputLatitude,double InputLongitude)
   /**
    ** Apply Latitude Bias Adjustment to CI value
    ** Inputs  : InitialCIValue - initial CI value
    **           InputLatitude  - current latitude of storm
    **           InputLongitude - current longitude of storm
    ** Outputs : None
    ** Return  : Adjusted MSLP value
    */
   {
      int RetErr;
      double ReturnCIPressureAdjValue = 0.0;

      boolean UseCKZTF = ADT_Env.UseCKZTF;
      if(!UseCKZTF) {
         double[] RetVals = adt_scenesearch(HistoryFileName,InitStrengthTF,LandFlagTF);
         int LatBiasAdjFlagID = (int)RetVals[0];
         double AdjustmentMultFactor = RetVals[1];
         ADT_History.IRCurrentRecord.LBflag = LatBiasAdjFlagID;
         /** System.out.printf("latbiasadjflagid=%d\n",LatBiasAdjFlagID); */ 
         if(LatBiasAdjFlagID>=2) {
            /** EIR scene */
            if((InputLatitude>=0.0)&&((InputLongitude>=-100.0)&&
               (InputLongitude<=-40.0))) {
               /** do not make adjustment in N Indian Ocean */
               ReturnCIPressureAdjValue = 0.0;
            } else {
               /** apply bias adjustment to pressure */
               ReturnCIPressureAdjValue = AdjustmentMultFactor*
                                        (7.325-(0.302*Math.abs(InputLatitude)));
            }
         }
      }

      return ReturnCIPressureAdjValue;
   }

   public static double[] adt_scenesearch(String HistoryFileName,boolean InitStrengthTF,boolean LandFlagTF)
   /**
    ** Search for valid scene range for Latitude Bias Adjustment application
    ** Inputs  : None
    ** Outputs : AdjustmentMultFactor_Return - multiplicative value for merging
    ** Return  : 0 - first record in new history file
    **           1 - non Enhanced Infrared scene or intermediate/merging application
    **           2 - entering/leaving valid adjustment time period.  Also used for
    **               non-history file intensity analysis (full adjustment)
    */
   {
      int LatBiasAdjFlagID = -1;
      double AdjustmentMultFactor = 0.0;

      int NumRecsHistory = ADT_History.HistoryNumberOfRecords();
      if(((NumRecsHistory==0)&&(InitStrengthTF))&&(HistoryFileName!=null)) {
         return new double[] { 0.0, -999.9 };
      }
      if(HistoryFileName==null) {
         return new double[] { 2.0, 1.0 };
      }

      int HistoryRecLatBiasAdjFlagIDValue = 0;

      int CloudScene = ADT_History.IRCurrentRecord.cloudscene;
      /** System.out.printf("cloudscene=%d\n",CloudScene); */
      if((CloudScene>=2)&&(CloudScene<6)) LatBiasAdjFlagID = 0;

      int ImageDate = ADT_History.IRCurrentRecord.date;
      int ImageTime = ADT_History.IRCurrentRecord.time;
      double CurrentTime = ADT_Functions.calctime(ImageDate,ImageTime);
      double CurrentTimeMinus6hr = CurrentTime - 0.26;
      double MergePeriodFirstTime = CurrentTime;

      int RecDate,RecTime,RecLand,RecLBFlag;
      double HistoryRecTime;
      double RecTnoRaw,RecCI;
      boolean LandCheckTF=true;
      boolean EIRSceneTypeTF=true;
      boolean FoundMergePeriodFirstRecordTF = false;
      double FirstHistoryRecTime = -99999.0;

      int XInc=0;
      while(XInc<NumRecsHistory) {
         RecDate = ADT_History.HistoryFile[XInc].date;
         RecTime = ADT_History.HistoryFile[XInc].time;
         HistoryRecTime = ADT_Functions.calctime(RecDate,RecTime);
         RecLand = ADT_History.HistoryFile[XInc].land;
         RecTnoRaw = ADT_History.HistoryFile[XInc].Traw;
         if(((LandFlagTF)&&(RecLand==1))||(RecTnoRaw<1.0)) {
            LandCheckTF = false;
         }
         if((HistoryRecTime<CurrentTime)&&(LandCheckTF)) {
            HistoryRecLatBiasAdjFlagIDValue = ADT_History.HistoryFile[XInc].LBflag;
            /** System.out.printf("Historyrectime=%f  Currtimem6=%f  eirscenetypetf=%b\n",HistoryRecTime,CurrentTimeMinus6hr,EIRSceneTypeTF); */
            if((HistoryRecTime>=CurrentTimeMinus6hr)&&(EIRSceneTypeTF)) {
               if(FirstHistoryRecTime<0.0) {
                 FirstHistoryRecTime = HistoryRecTime;
               }
               /** System.out.printf("HistoryRecLatBiasAdjFlagIDValue=%d\n",HistoryRecLatBiasAdjFlagIDValue); */
               if(HistoryRecLatBiasAdjFlagIDValue==0) {
                  EIRSceneTypeTF = false;
               } 
               if(HistoryRecLatBiasAdjFlagIDValue==2) {
                  if(!FoundMergePeriodFirstRecordTF) {
                     MergePeriodFirstTime = HistoryRecTime;
                     FoundMergePeriodFirstRecordTF = true;
                  }
               }
            }
         }
         XInc++;
      }

      /** System.out.printf("scenesearch: FirstHistoryRecTime=%f\n",FirstHistoryRecTime); */
      if(FirstHistoryRecTime<0.0) {
         /** there is a six hour gap in the data for some reason...
            I will use the last value available */
         LatBiasAdjFlagID=HistoryRecLatBiasAdjFlagIDValue;
         if(HistoryRecLatBiasAdjFlagIDValue>=1) {
            LatBiasAdjFlagID = 2;
         }
         AdjustmentMultFactor = 1.0;
      } else {
         /** System.out.printf("scenesearch: eirscenetypetf=%b",EIRSceneTypeTF); */
         if(EIRSceneTypeTF) {
            /** entering or in valid lat bias adjustment period */
            LatBiasAdjFlagID = 2;
            /** return value from 0 to 1 */
            AdjustmentMultFactor = (CurrentTime-MergePeriodFirstTime)/
                                   (CurrentTime-FirstHistoryRecTime);
         } else {
            /** LatBiasAdjFlagID = LatBiasAdjFlagID; */
            AdjustmentMultFactor = -999.0;
         }
      }
            
      return new double[] { (double)LatBiasAdjFlagID, AdjustmentMultFactor };
   }

}
