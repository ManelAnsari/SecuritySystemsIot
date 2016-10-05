#include <SoftwareSerial.h>
SoftwareSerial BT(10, 11);
int alarm;
int ledPin = 2;
int sensorPin = 3; //i tested with a push button-->hegazy
int speaker = 4;
int calibrationTime = 5;

//the time when the sensor outputs a low impulse
long unsigned int lowIn;

//the amount of milliseconds the sensor has to be low
//before we assume all motion has stopped
long unsigned int pause = 5000;

boolean lockLow = true;
boolean takeLowTime;

int pirPin = 5;//pir sensor pin -->Adel
int ledPin2=13;


int tempPin = 0;
int errorTemp = 10;

int calibrate = 0;
int reading;
boolean turnoff = false;

void setup() {
  //Serial.begin(9600);
  pinMode(ledPin, OUTPUT);//LED
  pinMode(sensorPin, INPUT);//sensor
  pinMode(speaker, INPUT);//speaker
  pinMode(pirPin, INPUT); //Pir sensor
  pinMode(ledPin2,OUTPUT);//Pir's led
  digitalWrite(pirPin, LOW); //
  digitalWrite(sensorPin, HIGH);

  Serial.begin(9600);
  Serial.println("calibrating");
  for (int i = 0; i < 30; i++) {
    Serial.print(".");
    delay(1000);
  }
  Serial.println("done");
  delay(50);
    BT.begin(9600);
    // Send test message to other device
    BT.println("Hello from Arduino");
  alarm = 0;
  

}


void loop() {
  
 //getting the voltage reading from the temperature sensor
 
 while(calibrate < 1000){
  reading = analogRead(sensorPin);  
  calibrate++;
  
 }
 
   reading = analogRead(sensorPin);  

 // converting that reading to voltage, for 3.3v arduino use 3.3
 float voltage = reading * 5.0;
 voltage /= 1024.0; 
 
 // print out the voltage
 Serial.print(voltage); Serial.println(" volts");
 
 // now print out the temperature
 float temperatureC = (voltage) * 10 + errorTemp;  //converting from 10 mv per degree wit 500 mV offset
                                               //to degrees ((voltage - 500mV) times 100)
 Serial.print(temperatureC); Serial.println(" degrees C");
 
 if(temperatureC > 30){
   startAlarm("Fire!!");
   turnoff = true;
   
  }else if(turnoff == true){
     stopAlarm();
    turnoff= false; 
   }
 
  
  
  int value_read_from_mobile;
    if (BT.available() > 0) //continuously try to read data from mobile and this condition means that the mobile actually sent something
      value_read_from_mobile = (BT.read() - '0');
    if (value_read_from_mobile == 1) { //flag value sent from mobile is 1
      stopAlarm();
    }
 else
  if (alarm == 1) { //means we are in alarming situation so make led blink and speaker produce alarm sound
    int sensorValue = analogRead(A0); // 0 – 1023 this value is read from potentiometer to control volume of alarm
    tone(4, sensorValue, 10);
    digitalWrite(ledPin, !digitalRead(ledPin));
    // delay to let it finish ‘tone’ instruction.
    delay(100);
  }


  if (digitalRead(sensorPin) == LOW) { //button press or in your case it will be sensor reading (i used it for test-->hegazy)
    char str[] = "Button Pressed!";
    startAlarm(str);
  }


  if (digitalRead(pirPin) == HIGH) {
     digitalWrite(ledPin2, HIGH);   //the led visualizes the sensors output pin state
    if (lockLow) {
      lockLow = false;
      Serial.println("---");
      Serial.print("motion detected at ");
      Serial.print(millis() / 1000);
      Serial.println(" sec");
      //startAlarm("Motion detected");
      delay(50);
    }
    takeLowTime = true;
  }
  if (digitalRead(pirPin) == LOW) {
     digitalWrite(ledPin2, LOW);  //the led visualizes the sensors output pin state
    if (takeLowTime) {
      lowIn = millis();          //save the time of the transition from high to LOW
      takeLowTime = false;       //make sure this is only done at the start of a LOW phase
    }
    if (!lockLow && millis() - lowIn > pause) {
      //makes sure this block of code is only executed again after
      //a new motion sequence has been detected
      lockLow = true;
      Serial.print("motion ended at ");      //output
      Serial.print((millis() - pause) / 1000);
      Serial.println(" sec");
//      stopAlarm();
      delay(50);
    }

  }
}


void startAlarm(char msg[]) { //in case of sensor reads danger values it will send message to mobile and will make sound from speaker and blink the LED
  alarm = 1;
  BT.println(msg);
}
void stopAlarm() {
  alarm = 0;
  digitalWrite(ledPin, LOW);
}
