package edu.wisc.ssec.mcidasv.control.adt;

@SuppressWarnings("unused")

public class ADT_Output {

   static String[] Months = { "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC" };
   static String[] LatNS = {"N","S"};        /** N/S hemisphere character value */
   static String[] LonWE = {"W","E"};        /** E/W hemisphere character value */
    /** Rule 9 string array */
   static String[] Rule9String = { "OFF   ","ON    ","WEAKEN" };
    /** rapid dissipation string array */
   static String[] RapidString = { "OFF   ","FLAG  ","ON    ","ON    " };
    /** BD curve value string array */
   static String[] BDCatString = { "LOW CLD","OFF WHT","DK GRAY","MD GRAY", "LT GRAY","BLACK  ","WHITE  " };
    /** ocean basin string array */
   static String[] BasinString = { "ATLANTIC    ","WEST PACIFIC", "EAST PACIFIC","INDIAN      " };
    /** eye scene type array */
   static String[] EyeSceneTypes = { "EYE","PINHOLE EYE","LARGE EYE","NONE" };
    /** cloud scene type array */
   static String[] CloudSceneTypes = { "UNIFORM CDO","EMBEDDED CENTER","IRREGULAR CDO","CURVED BAND","SHEAR","EYE" };
    /** storm position type arrays */
   static String[] AutoPosString = { "MANUAL", "FORECAST INTERPOLATION", "LAPLACIAN ANALYSIS", "WARMEST PIXEL SEARCH",
                                     "SPIRAL ANALYSIS", "RING/SPIRAL COMBINATION", "LINEAR EXTRAPOLATION",
                                     "NETCDF NOMINAL POSITION", "NOT AVAILABLE"};
   static String[] AutoPosStringAbbr = { " MAN ","FCST ", "LAPL ", "WARM ", "SPRL ", "COMBO", "EXTRP", "NETCDF", " N/A " };
    /** basin ID string array */
   static String[] PW_BasinValues = { "ATLANTIC","PACIFIC " };
    /** Rule 8 string array */
   static String[] Rule8String = { "NO LIMIT ","0.5T/6hr ","1.0T/6hr ","1.7T/12hr",
                                   "2.2T/18hr","2.7T/24hr","         ","         ",
                                   "0.2T/hour","0.5T/hour",
                                  "NO LIMIT ","0.5T/6hr ","1.0T/6hr ","2.7T/12hr",
                                   "3.2T/18hr","3.7T/24hr","         ","         ",
                                   "0.2T/hour","0.5T/hour",
                                  "NO LIMIT ","0.5T/6hr ","0.7T/6hr ","1.2T/12hr",
                                   "1.7T/18hr","2.2T/24hr","         ","         ",
                                   "0.2T/hour","0.5T/hour",
                                   "MW Adjst ","MW ON    ","MW ON    ","MW HOLD  ",
                                   "MW AdjEnd" };

   public ADT_Output() {
   }

   public static String TextScreenOutput(String HistoryFileName){

       System.err.println("TextScreenOutput() in...");
      String TextScreen_Return = "";

      /** int LatNSval = 0; */
      /** int LonWEval = 0; */
      String SceneString = "";
      String RadiusMaxWindString = "";
      String SceneMaxCBString = "";
      String SceneMaxCBLLString = "";
      String SceneMaxCBString2 = "";
      String SceneMaxCBLLString2 = "";
      double PresWindValue_Pressure = -999.0;
      double PresWindValue_Wind = -999.0;
      boolean MaxCurvedBandTF = false;
      boolean RadiusMaxWindTF = false;

      /* convert Julian date/time to DateValue/month/year format */
      int CurDate = ADT_History.IRCurrentRecord.date;
      int CurTime = ADT_History.IRCurrentRecord.time;
      double CurLatitudeValue = ADT_History.IRCurrentRecord.latitude;
      double CurLongitudeValue = ADT_History.IRCurrentRecord.longitude;
      int[] ReturnValues = ADT_Functions.adt_yddmy(CurDate);
      int DateValue = ReturnValues[0];
      int MonthValue = ReturnValues[1];
      int YearValue = ReturnValues[2];

      /** format character string for date output */
      String DateString = String.format("%02d %3s %04d",DateValue,Months[MonthValue-1],YearValue);

      /** format character string for time output */
      String TimeString = String.format("  %06d UTC",CurTime);
    
      /* convert xx.xxxx latitude format to degree/minute/second format */
      int[] ReturnValues2A = adt_lldms(CurLatitudeValue);
      int DegreeValue2A = ReturnValues2A[0];
      int MinuteValue2A = ReturnValues2A[1];
      int SecondValue2A = ReturnValues2A[2];
      int LatNSval = (CurLatitudeValue<0.0) ? 1 : 0;
      /** 
      LatNSval=0;
      if(CurLatitudeValue<0.0) {
         LatNSval=1;
      }
      */
      /** format character string for latitude output */
      String LatitudeString = String.format("%3d:%02d:%02d %1s",DegreeValue2A,MinuteValue2A,SecondValue2A,LatNS[LatNSval]);

      int[] ReturnValues2B = adt_lldms(CurLongitudeValue);
      int DegreeValue2B = ReturnValues2B[0];
      int MinuteValue2B = ReturnValues2B[1];
      int SecondValue2B = ReturnValues2B[2];
      /** int LonWEval = (CurLongitudeValue<0.0) ? 1 : 0; old McIDAS-X conversion */
      int LonWEval = (CurLongitudeValue<0.0) ? 0 : 1;
      /** 
      LonWEval=0;
      if(CurLongitudeValue<0.0) {
         LonWEval=1;
      }
      */
      /** format character string for latitude output */
      String LongitudeString = String.format("%3d:%02d:%02d %1s",DegreeValue2B,MinuteValue2B,SecondValue2B,LonWE[LonWEval]);

      int[] ReturnValues3 = ADT_Functions.adt_oceanbasin(CurLatitudeValue,CurLongitudeValue);
      int BasinIDValue = ReturnValues3[0];
      int DomainID = ReturnValues3[1];
    
      /** determine Dvorak pressure/wind speed in relation to final CI # */
      double CurRawT = ADT_History.IRCurrentRecord.Traw;
      double CurRawTorig = ADT_History.IRCurrentRecord.TrawO;
      double CurFinalT = ADT_History.IRCurrentRecord.Tfinal;
      double CurCI = ADT_History.IRCurrentRecord.CI;
      double CurCIAdjP = ADT_History.IRCurrentRecord.CIadjp;
      /** System.out.printf("ciadjp=%f\n",CurCIAdjP); */
      PresWindValue_Pressure = ADT_Functions.adt_getpwval(0,CurCI,CurLatitudeValue,CurLongitudeValue);
      PresWindValue_Wind = ADT_Functions.adt_getpwval(1,CurCI,CurLatitudeValue,CurLongitudeValue);

      boolean Vmax1or10TF = ADT_Env.Vmax1or10TF;

      if(!Vmax1or10TF) {
         /** convert 1-minute to 10-minute average Vmax for output */
         PresWindValue_Wind=0.88*PresWindValue_Wind;
      }
    
      /** determine Rule 8 and Rule 9 screen output values */
      int Rule8Value = ADT_History.IRCurrentRecord.rule8;
      int Rule9Value = ADT_History.IRCurrentRecord.rule9;
      int RapidIntenValue = ADT_History.IRCurrentRecord.rapiddiss;

      double EyeTempValue = ADT_History.IRCurrentRecord.eyet;
      double CloudTempValue = ADT_History.IRCurrentRecord.cloudt;

      /** determine scenetype to be output to screen */
      int EyeSceneTypeValue = ADT_History.IRCurrentRecord.eyescene;
      int CloudSceneTypeValue = ADT_History.IRCurrentRecord.cloudscene;
      int CloudFFTValue = ADT_History.IRCurrentRecord.cloudfft;
      if(CloudSceneTypeValue==2) {
         SceneString = String.format("%s",CloudSceneTypes[CloudSceneTypeValue]);
      } else if(CloudSceneTypeValue==3) {
         double CurvedBandValue = ((double)(ADT_History.IRCurrentRecord.ringcbval-1))/24.0;
         double CurvedBandMaxValue = ((double)(ADT_History.IRCurrentRecord.ringcbvalmax-1))/25.0;
         int RingCB = ADT_History.IRCurrentRecord.ringcb;
         SceneString = String.format("CURVED BAND with %4.2f ARC in %s",CurvedBandValue,BDCatString[RingCB]);
         /** System.out.printf("curved band max=%f curved band=%f\n",CurvedBandMaxValue,CurvedBandValue); */
         if(CurvedBandMaxValue>CurvedBandValue) {
            MaxCurvedBandTF = true;
         }
         if(MaxCurvedBandTF) {
            SceneMaxCBString = String.format("Maximum CURVED BAND with %4.2f ARC in %s",CurvedBandMaxValue,BDCatString[RingCB]);
            /** convert xx.xxxx latitude format to degree/minute/second format */
            double CurvedBandMaxLatitude = ADT_History.IRCurrentRecord.ringcbvalmaxlat;
            int[] ReturnValues4A = adt_lldms(CurvedBandMaxLatitude);
            int DegreeValue4A = ReturnValues4A[0];
            int MinuteValue4A = ReturnValues4A[1];
            int SecondValue4A = ReturnValues4A[2];
            LatNSval = (CurLatitudeValue<0.0) ? 1 : 0;
            /**
            LatNSval=0;
            if(CurvedBandMaxLatitude<0.0) {
               LatNSval=1;
            }
            */
            /** format character string for latitude output */
            String CBMaxLatString = String.format("%3d:%02d:%02d %1s",DegreeValue4A,MinuteValue4A,SecondValue4A,LatNS[LatNSval]);

            /** convert xx.xxxx longitude format to degree/minute/second format */
            double CurvedBandMaxLongitude = ADT_History.IRCurrentRecord.ringcbvalmaxlon;
            int[] ReturnValues4B = adt_lldms(CurvedBandMaxLongitude);
            int DegreeValue4B = ReturnValues4B[0];
            int MinuteValue4B = ReturnValues4B[1];
            int SecondValue4B = ReturnValues4B[2];
            /** LonWEval = (CurLongitudeValue<0.0) ? 1 : 0;  old conversion for McIDAS-X */
            LonWEval = (CurLongitudeValue<0.0) ? 0 : 1;
            /**
            LonWEval=0;
            if(CurvedBandMaxLongitude<0.0) {
               LonWEval=1;
            }
            */
            /** format character string for longitude output */
            String CBMaxLonString = String.format("%3d:%02d:%02d %1s",DegreeValue4B,MinuteValue4B,SecondValue4B,LonWE[LonWEval]);
            SceneMaxCBLLString = String.format(" at Lat:%12s  Lon:%12s",CBMaxLatString,CBMaxLonString);
         }
      }
      else if(CloudSceneTypeValue==4) {
         double CDOSizeValue = ADT_History.IRCurrentRecord.eyecdosize/110.0;
         if(CDOSizeValue<1.30) {
            SceneString = String.format("SHEAR (%4.2f^ TO DG)",CDOSizeValue);
         } else {
            SceneString = String.format("SHEAR (>1.25^ TO DG)");
         }
      }
      else {
         if(EyeSceneTypeValue<=2) {
            SceneString = String.format("%s ",EyeSceneTypes[EyeSceneTypeValue]);
            if(EyeSceneTypeValue<=2) {
               RadiusMaxWindTF = true;
            }
            double RMWValue = ADT_History.IRCurrentRecord.rmw;
            if(RMWValue<0.0) {
               if(EyeSceneTypeValue==1) {
                  RadiusMaxWindString = String.format("<10");
                   
               } else {
                  RadiusMaxWindString = String.format("N/A");
               }
            } else {
               RadiusMaxWindString = String.format("%3d",(int)RMWValue);
            }
         }
         else {
            if((Rule8Value==31)||(Rule8Value==32)) {
               SceneString = String.format("%s CLOUD REGION w/ MW EYE",CloudSceneTypes[CloudSceneTypeValue]);
            } else {
               SceneString = String.format("%s CLOUD REGION",CloudSceneTypes[CloudSceneTypeValue]);
            }
         }
      }
      
      int NumRecsHistory = ADT_History.HistoryNumberOfRecords();
      double InitStrengthValue = ADT_Env.InitRawTValue;
      if((HistoryFileName!=null)&&(NumRecsHistory==0)) {
         CloudSceneTypeValue=0;
         if(InitStrengthValue>1.0) {
            SceneString = String.format("USER DEFINED INITIAL CLASSIFICATION");
         } else {
            if(InitStrengthValue==1.0) {
               SceneString = String.format("INITIAL CLASSIFICATION");
            }
         }
      }
      if(InitStrengthValue<0.0) {
         SceneString = String.format("USER REINITIALIZED CLASSIFICATION");
      }
      String SceneString2 = String.format("%s",SceneString);
      if(MaxCurvedBandTF) {
         SceneMaxCBString2 = String.format("%s",SceneMaxCBString);
         SceneMaxCBLLString2 = String.format("%s",SceneMaxCBLLString);
      }

      int SatType = ADT_History.IRCurrentRecord.sattype;
      int AutoPos = ADT_History.IRCurrentRecord.autopos;
      int LandFlag = ADT_History.IRCurrentRecord.land;
      int R34Value = ADT_History.IRCurrentRecord.r34;
      int MSLPenvValue = ADT_History.IRCurrentRecord.MSLPenv;
      double VZAValue = ADT_History.IRCurrentRecord.vza;
      String SatelliteIDString = ADT_Functions.adt_sattypes(SatType);
      
      String VersionString = ADT_Env.ADTVersion;
      boolean LandFlagTF = ADT_Env.LandFlagTF;
      boolean UseCKZTF = ADT_Env.UseCKZTF;

      /* send results to the screen */

      TextScreen_Return += String.format("\n****************************************************\n\n");
      TextScreen_Return += String.format("                     UW - CIMSS                     \n");
      TextScreen_Return += String.format("              ADVANCED DVORAK TECHNIQUE       \n");
      TextScreen_Return += String.format("                  %17s                \n",VersionString);
      TextScreen_Return += String.format("         Tropical Cyclone Intensity Algorithm       \n\n");
      TextScreen_Return += String.format("             ----- Current Analysis ----- \n");
      TextScreen_Return += String.format("     Date : %12s    Time : %12s\n",DateString,TimeString);
      TextScreen_Return += String.format("      Lat : %12s     Lon : %12s\n\n",LatitudeString,
                                                           LongitudeString);
      if((LandFlagTF)&&(LandFlag==1)) {
         TextScreen_Return += String.format("              TROPICAL CYCLONE OVER LAND\n");
         TextScreen_Return += String.format("              NO ADT ANALYSIS AVAILABLE\n");
      }
      else {
         if(Vmax1or10TF) {
            TextScreen_Return += String.format("                CI# /Pressure/ Vmax\n");
         } else {
            TextScreen_Return += String.format("                CI# /Pressure/ Vmax(10-min)\n");
         }
         if(UseCKZTF) {
            TextScreen_Return += String.format("                %3.1f /%6.1fmb/%5.1fkt\n\n",CurCI,
                                               PresWindValue_Pressure,PresWindValue_Wind);
         } else {
            TextScreen_Return += String.format("                %3.1f /%6.1fmb/%5.1fkt\n\n",CurCI,
                                               PresWindValue_Pressure+CurCIAdjP, PresWindValue_Wind);
         }
         if(HistoryFileName!=null) {
            TextScreen_Return += String.format("             Final T#  Adj T#  Raw T# \n");
            TextScreen_Return += String.format("                %3.1f     %3.1f     %3.1f\n\n",CurFinalT,CurRawT,CurRawTorig);
         }
         if(!UseCKZTF) {
            TextScreen_Return += String.format("     Latitude bias adjustment to MSLP : %+5.1fmb\n\n",CurCIAdjP);
         }
         if(RadiusMaxWindTF) {
            TextScreen_Return += String.format(" Estimated radius of max. wind based on IR :%3s km\n\n",RadiusMaxWindString);
         }
         TextScreen_Return += String.format(" Center Temp : %+5.1fC    Cloud Region Temp : %5.1fC\n",EyeTempValue,CloudTempValue);
         TextScreen_Return += String.format("\n Scene Type : %s \n",SceneString2);
         if(MaxCurvedBandTF) {
            TextScreen_Return += String.format("              %s \n",SceneMaxCBString2);
            TextScreen_Return += String.format("              %s \n",SceneMaxCBLLString2);
         }
         TextScreen_Return += String.format("\n Positioning Method : %s \n",AutoPosString[AutoPos]);
         TextScreen_Return += String.format("\n Ocean Basin : %12s  \n",BasinString[BasinIDValue]);
         TextScreen_Return += String.format(" Dvorak CI > MSLP Conversion Used : %8s  \n",PW_BasinValues[DomainID]);
         if(HistoryFileName!=null) {
            TextScreen_Return += String.format("\n Tno/CI Rules : Constraint Limits : %9s\n", Rule8String[Rule8Value]);
            TextScreen_Return += String.format("                   Weakening Flag : %6s\n", Rule9String[Rule9Value]);
            TextScreen_Return += String.format("           Rapid Dissipation Flag : %6s\n", RapidString[RapidIntenValue]);
         }
         if(UseCKZTF) {
            TextScreen_Return += String.format("\n C/K/Z MSLP Estimate Inputs :\n");
            if(R34Value>0.0) {
               TextScreen_Return += String.format("  - Average 34 knot radii : %4dkm\n",R34Value);
            } else {
               TextScreen_Return += String.format("  - Average 34 knot radii : N/A\n");
            }
            TextScreen_Return += String.format("  - Environmental MSLP    : %4dmb\n",MSLPenvValue);
         }
         TextScreen_Return += String.format("\n Satellite Name : %7s \n",SatelliteIDString);
         TextScreen_Return += String.format(" Satellite Viewing Angle : %4.1f degrees \n",VZAValue);
      }
      TextScreen_Return += String.format("\n****************************************************\n\n");

      System.err.println("TextScreenOutput() out, val: " + TextScreen_Return);
      return TextScreen_Return;
   }

   public static int[] adt_lldms(double FullDegreeValue)
   /**
    ** Convert "degree.partial_degree" to degree/minute/second format.
    ** Inputs  : FullDegreeValue_Input  - latitude/longitude value to convert
    ** Outputs : Degree_Return - degrees integer value
    **           Minute_Return - minutes integer value
    **           Second_Return - seconds integer value
    ** Return  : None
    */
   {
      int DegreeValue = (int)FullDegreeValue;
      double MinuteValue = (FullDegreeValue-(double)DegreeValue)*60.0;
      double SecondValue = (MinuteValue-(double)((int)MinuteValue))*60.0;
      int DegreeReturn = Math.abs(DegreeValue);
      int MinuteReturn = (int)Math.abs(MinuteValue);
      int SecondReturn = (int)Math.abs(SecondValue);

      return new int[] { DegreeReturn, MinuteReturn, SecondReturn };
   }
}
