Tyrata Mobile Development Timeline
===

# Table of Contents
[TOC]

# V 1.0.0
:::info
- **Developer:** Srikar
- **Evaluator:** Aaron
- **Date Opened:** Nov 1, 2018
- **Date Closed:** Mar 27, 2019
- **Participants:**
    - Srikar
    - Aaron
    - Dave
- **Sign-Off:** Aaron <aaron.franklin@tyrata.com>
- **Summary:** Initial Development of Screens and BLE capabilities.
:::

## Current Open Issues
---
- No testing has been done yet.
---
## Proposed Updates
---
- Screens for reading sensor data and changing the sensors settings.
---
## Response to Update Proposal
---
- Built out the screens and got BLE working.
---
## Deliverables
---
### DEV:
- [x] Build out screens for testing the sensors.

### QA
- [x] When new sensors arrive, develop bluetooth Characteristics.
---

## Notes 
---
- Need to start testing the app.
---

# V 1.1.0
:::info
- **Developer:** Luke
- **Evaluator:** Aaron
- **Date Opened:** Mar 27, 2019
- **Date Closed:** Apr 05, 2019
- **Participants:**
    - Luke
    - Aaron
    - Dave
- **Sign-Off:** Aaron <aaron.franklin@tyrata.com>
- **Summary:** Addition of new screens for AD7747 and RF measurements.
:::

## Current Open Issues
---
- New sensors will have two modes. We need to be able to change settings for each mode as well as interpret the responses of the data into a graph.
---
## Proposed Updates
---
- Two new modes for the sensor. Keep the same look of the original.
---
## Response to Update Proposal
---
- Screens were built, no functionality yet.
---
## Deliverables
---
### DEV:
- [x] Build out screen for AD7747.
- [x] Build out screen for RF.

### QA
- [x] Make sure titles of different labels and axes are correct.
- [x] Test out the zooming on graph.
- [x] Test out data formatting.
---

## Notes 
---
- Still have not been able to work with bluetooth.
---
# V 1.1.1
:::info
- **Developer:** Luke
- **Evaluator:** Aaron
- **Date Opened:** Apr 05, 2019
- **Date Closed:** Apr 07, 2019
- **Participants:**
    - Luke
    - Aaron
    - Dave
- **Sign-Off:** Aaron <aaron.franklin@tyrata.com>
- **Summary:** Addition of new screens for AD7747 and RF measurements.
:::

## Current Open Issues
---
- Want to be able to change specific settings for each sensor. Need to develop screens for the settings of each sensor.
---
## Proposed Updates
---
- Proposed layout for sensor settings
- Sending UART commands to sensor to change specific settings.
---
## Response to Update Proposal
---
- Screens were built, no UART capability yet. Need sensor to test UART.
---
## Deliverables
---
### DEV:
- [x] Build out settings screen for AD7747.
- [x] Build out settings screen for RF.

### QA
- [x] List of commands needed for AD7747.
- [x] List of commands needed for RF.
---

## Notes 
---
- Still have not been able to work with bluetooth.
---

# V 2.0.0
:::info
- **Developer:** Luke
- **Evaluator:** Aaron
- **Date Opened:** Apr 07, 2019
- **Date Closed:** May 17, 2019
- **Participants:**
    - Luke
    - Aaron
    - Dave
- **Sign-Off:** Aaron <aaron.franklin@tyrata.com>
- **Summary:** Initial Development of Screens and BLE capabilities.
:::

## Current Open Issues
---
- Still no bluetooth functionality.
---
## Proposed Updates
---
- Connect to UART.
---
## Response to Update Proposal
---
- Current version of the app had no bluetooth capabilities. Redesigning app but keep current styling.
- The data services (BluetoothLeService, Reading, Firestore) are not equipt for any real functionality. Will recreate all backend functionality.
- Rebuild view adapters for new services.
---
## Deliverables
---
### DEV:
- [x] Recreate Bluetooth Functionality.
    - [x] Create BLE Scanning
    - [x] Create UART Connection
    - [x] Send UART Commands
    - [x] Test
- [x] Build Hardware Test
    - [x] Set up and Program NORDIC_DEV Board
    - [x] Connect to App
- [x] Build New Screens to Connect to New Services
    - [x] Sensor List Screen 
    - [x] Sensor Readings Screen
    - [x] Sensor Settings Screen

### QA
- [x] Provide Hardware to test
---

## Notes 
---
- Ready to begin testing on Tyrata AD7747 Sensor.
---

# V 2.1.0
:::info
- **Developer:** Luke
- **Evaluator:** Michael
- **Date Opened:** Nov 1, 2018
- **Date Closed:** Mar 27, 2019
- **Participants:**
    - Luke
    - Michael
- **Sign-Off:** Michael <michael.stangler@tyrata.com>
- **Summary:** First Proposed Updates After successful pairing.
:::

## Current Open Issues
---
- None.
---
## Proposed Updates
---
- Cap Dac A -- Send and Read buttons - for read return the 6 LSBs (ie. bit 0-5), for write, allow to set 6 LSBs (bit 0-5) and bit 7 will be set by the previous value of bit 7 from last CAP DAC read. Note: bit 6 can remain 0 as it is required to be for operation   
![image]
- Export Data to .csv, once that's done work on saving data locally
    - Include Date/Time, Sensor, Voltage, Temp, Capacitance
- Automatically pair to device on entry to Sensor Info screen
- Plot -- Decrease size of plot to allow list of data to be larger. Look at potentially having dropdown for x-axis to switch between measurement number and time since device powered up.
- Move settings from list, ie. the settings shape next to the reported capacitance seems to cause app to crash. Potential move ability to change device name to top of main page /or settings page.

---
## Response to Update Proposal
---
- Capacitance Out of Range due to parsing issue with reading in register values and scan values in AD7747Activity.java method handleIncoming().
- Device name issue due to incorrect command being passed. “ID=” not “DEVICE=”
- Remember to enable Storage Permissions on the app to allow exporting data.
- Translate from mC to C and display in the handleTempData() method in AD7747Activity.java
- Duplicated button functionality for CapDacA. Made Set inactive and overwrote read with set in AD7747SettingsActivity.java

---
## Deliverables
---
### DEV:
- [x] CapDacA
    [x] Building the Send Button
    [x] Building the Send Functionality
    [x] Building the Read Button
    [x] Building the Read Functionality
- [x] Pair Device on Entry to AD7747Activity
- [x] Move Device Renaming to Top of Page
- [x] Shrink Plot
    [ ] Drop Down Axis Label
- [ ] Save Data To CSV

### QA
- [x] Test CapDacA Read Button.
- [x] Test CapDacA Set Button.
- [x] Test Renaming Sensor.
- [x] Test CSV Export
- [x] Feedback on Plotting Size.
---

## Notes 
---
- When Capacitance is out of range, App Crashes	
```	
I/System.out: SEND ME THIS LINE MICHAEL (Response): <pf>AD7747 out of ra
D/AndroidRuntime: Shutting down VM
E/AndroidRuntime: FATAL EXCEPTION: main
    Process: com.tyrata.tyrata, PID: 5270
    java.lang.ArrayIndexOutOfBoundsException: length=1; index=1
```
- Change device name – doesn’t crash app, however does not change the name of the device
- BLE connects when Tyrata device is selected
- Export data – opens up email, however, no data is actually sent in the email
- Get Temperature button – reports in mC, not C
-  Read CAPDAC A button – does not read but rather prompts to sets CAP DAC A
```
E/AndroidRuntime: FATAL EXCEPTION: main
    Process: com.tyrata.tyrata, PID: 8548
    java.lang.NumberFormatException: For input string: "2F"
```
- Set CAPDAC A button – nonfuctional
---
# V 2.1.1
:::info
- **Developer:** Srikar
- **Evaluator:** Aaron
- **Date Opened:** Nov 1, 2018
- **Date Closed:** Mar 27, 2019
- **Participants:**
    - Srikar
    - Aaron
    - Dave
- **Sign-Off:** Aaron <aaron.franklin@tyrata.com>
- **Summary:** Initial Development of Screens and BLE capabilities.
:::

## Current Open Issues
---
- No testing has been done yet.
---
## Proposed Updates
---
- Screens for reading sensor data and changing the sensors settings.
---
## Response to Update Proposal
---
- Built out the screens and got BLE working.
---
## Deliverables
---
### DEV:
- [x] Build out screens for testing the sensors.

### QA
- [x] When new sensors arrive, develop bluetooth Characteristics.
---

## Notes 
---


---
# V 2.1.2
:::info
- **Developer:** Srikar
- **Evaluator:** Aaron
- **Date Opened:** Nov 1, 2018
- **Date Closed:** Mar 27, 2019
- **Participants:**
    - Srikar
    - Aaron
    - Dave
- **Sign-Off:** Aaron <aaron.franklin@tyrata.com>
- **Summary:** Initial Development of Screens and BLE capabilities.
:::

## Current Open Issues
---
- No testing has been done yet.
---
## Proposed Updates
---
- Screens for reading sensor data and changing the sensors settings.
---
## Response to Update Proposal
---
- Built out the screens and got BLE working.
---
## Deliverables
---
### DEV:
- [x] Build out screens for testing the sensors.

### QA
- [x] When new sensors arrive, develop bluetooth Characteristics.
---

## Notes 
---
- Need to start testing the app.
---
# V 2.2.0
:::info
- **Developer:** Srikar
- **Evaluator:** Aaron
- **Date Opened:** Nov 1, 2018
- **Date Closed:** Mar 27, 2019
- **Participants:**
    - Srikar
    - Aaron
    - Dave
- **Sign-Off:** Aaron <aaron.franklin@tyrata.com>
- **Summary:** Initial Development of Screens and BLE capabilities.
:::

## Current Open Issues
---
- No testing has been done yet.
---
## Proposed Updates
---
- Screens for reading sensor data and changing the sensors settings.
---
## Response to Update Proposal
---
- Built out the screens and got BLE working.
---
## Deliverables
---
### DEV:
- [x] Build out screens for testing the sensors.

### QA
- [x] When new sensors arrive, develop bluetooth Characteristics.
---

## Notes 
---
- Need to start testing the app.
---
