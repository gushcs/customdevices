/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *  Customisations: 17-10-2017   Icons and colour states
 */

metadata {

	definition (name: "Z-Wave First Alert Smoke Alarm", namespace: "castlecole", author: "SmartThings") {
		capability "Smoke Detector"
		capability "Sensor"
		capability "Battery"
		capability "Health Check"
		attribute  "lastCheckin", "string"

		fingerprint deviceId: "0xA100", inClusters: "0x20,0x80,0x70,0x85,0x71,0x72,0x86"
		fingerprint mfr:"0138", prod:"0001", model:"0001", deviceJoinName: "First Alert Smoke Detector - Custom"
	}

	simulator {
		status "smoke": "command: 7105, payload: 01 FF"
		status "clear": "command: 7105, payload: 01 00"
		status "test": "command: 7105, payload: 0C FF"
 		status "battery 100%": "command: 8003, payload: 64"
		status "battery 5%": "command: 8003, payload: 05"
	}

	tiles (scale: 2){
		multiAttributeTile(name:"smoke", type:"generic", width:6, height:4){
			tileAttribute ("device.smoke", key:"PRIMARY_CONTROL") {
				attributeState("clear", label:"CLEAR", icon:"https://raw.githubusercontent.com/castlecole/customdevices/master/alarm-clear0.png", backgroundColor:"#359148")
				attributeState("detected", label:"SMOKE", icon:"https://raw.githubusercontent.com/castlecole/customdevices/master/alarm-notclear0.png", backgroundColor:"#ed0000")
				attributeState("tested", label:"TEST", icon:"https://raw.githubusercontent.com/castlecole/customdevices/master/alarm-notclear0.png", backgroundColor:"#e86d13")
			}
			tileAttribute("device.lastCheckin", key: "SECONDARY_CONTROL") {
				attributeState("default", label:'Last Checkin: ${currentValue}', icon: "st.Health & Wellness.health9")
			}
		}

		valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
            	state "default", label:'${currentValue}%'+"\n", unit:"", icon:"https://raw.githubusercontent.com/castlecole/Xiaomi/master/Battery.png", 
            		backgroundColors: [
				[value: 0, color: "#ff1800"],
				[value: 10, color: "#fb854a"],
				[value: 25, color: "#ceec24"],
				[value: 50, color: "#71f044"],
				[value: 75, color: "#33d800"]
            		]
		}

		standardTile("info", "device.smoke", inactiveLabel: True, decoration: "flat", width:2, height:2) {
			state "clear", icon:"", label: "All Clear"
			state "smoke", icon:"", label: "Fire! Fire! Fire!"
			state "tested", icon:"", label: "Alarm Test"
		}
		
		standardTile("refresh", "device.refresh", inactiveLabel: False, decoration: "flat", width: 2, height: 2) {
			state "default", action:"refresh.refresh", icon:"https://raw.githubusercontent.com/castlecole/customdevices/master/refresh.png"
        	}

 		multiAttributeTile(name:"smoke2", type:"generic"){
			tileAttribute ("device.smoke", key:"PRIMARY_CONTROL") {
				attributeState("clear", label:"CLEAR", icon:"https://raw.githubusercontent.com/castlecole/customdevices/master/House-Normal.png", backgroundColor:"#359148")
				attributeState("detected", label:"SMOKE", icon:"https://raw.githubusercontent.com/castlecole/customdevices/master/House-Fire.png", backgroundColor:"#ed0000")
				attributeState("tested", label:"TEST", icon:"https://raw.githubusercontent.com/castlecole/customdevices/master/House-fire.png", backgroundColor:"#e86d13")
			}
		}

		main "smoke2"
		details(["smoke", "battery", "info", "refresh"])
	}
}

def installed() {

// Device checks in every hour, this interval allows us to miss one check-in notification before marking offline
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
	def cmds = []
	createSmokeEvents("smokeClear", cmds)
	cmds.each { cmd -> sendEvent(cmd) }
}

def updated() {
// Device checks in every hour, this interval allows us to miss one check-in notification before marking offline
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID])
}

def parse(String description) {
	def results = []
	
	//  send event for heartbeat    
  	def now = new Date().format("yyyy MMM dd EEE h:mm:ss a", location.timeZone)
  	sendEvent(name: "lastCheckin", value: now)
	
	if (description.startsWith("Err")) {
	    results << createEvent(descriptionText:description, displayed:true)
	} else {
		def cmd = zwave.parse(description, [ 0x80: 1, 0x84: 1, 0x71: 2, 0x72: 1 ])
		if (cmd) {
			zwaveEvent(cmd, results)
		}
	}
	log.debug "'$description' parsed to ${results.inspect()}"
	return results
}

def createSmokeEvents(name, results) {
	def text = null
	switch (name) {
		case "smoke":
			text = "$device.displayName smoke was detected!"
			// these are displayed:false because the composite event is the one we want to see in the app
			results << createEvent(name: "smoke",          value: "detected", descriptionText: text)
			break
		case "tested":
			text = "$device.displayName was tested"
			results << createEvent(name: "smoke",          value: "tested", descriptionText: text)
			break
		case "smokeClear":
			text = "$device.displayName smoke is clear"
			results << createEvent(name: "smoke",          value: "clear", descriptionText: text)
			name = "clear"
			break
		case "testClear":
			text = "$device.displayName test cleared"
			results << createEvent(name: "smoke",          value: "clear", descriptionText: text)
			name = "clear"
			break
	}
}

def zwaveEvent(physicalgraph.zwave.commands.alarmv2.AlarmReport cmd, results) {
	if (cmd.zwaveAlarmType == physicalgraph.zwave.commands.alarmv2.AlarmReport.ZWAVE_ALARM_TYPE_SMOKE) {
		if (cmd.zwaveAlarmEvent == 3) {
			createSmokeEvents("tested", results)
		} else {
			createSmokeEvents((cmd.zwaveAlarmEvent == 1 || cmd.zwaveAlarmEvent == 2) ? "smoke" : "smokeClear", results)
		}
	} else switch(cmd.alarmType) {
		case 1:
			createSmokeEvents(cmd.alarmLevel ? "smoke" : "smokeClear", results)
			break
		case 12:  // test button pressed
			createSmokeEvents(cmd.alarmLevel ? "tested" : "testClear", results)
			break
		case 13:  // sent every hour -- not sure what this means, just a wake up notification?
			if (cmd.alarmLevel == 255) {
				results << createEvent(descriptionText: "$device.displayName checked in", isStateChange: false)
			} else {
				results << createEvent(descriptionText: "$device.displayName code 13 is $cmd.alarmLevel", isStateChange:true, displayed:false)
			}
			
			// Clear smoke in case they pulled batteries and we missed the clear msg
			if(device.currentValue("smoke") != "clear") {
				createSmokeEvents("smokeClear", results)
			}

			// Check battery if we don't have a recent battery event
			if (!state.lastbatt || (now() - state.lastbatt) >= 48*60*60*1000) {
				results << response(zwave.batteryV1.batteryGet())
			}
			break
		default:
			results << createEvent(displayed: true, descriptionText: "Alarm $cmd.alarmType ${cmd.alarmLevel == 255 ? 'activated' : cmd.alarmLevel ?: 'deactivated'}".toString())
			break
	}
}

// SensorBinary and SensorAlarm aren't tested, but included to preemptively support future smoke alarms
//
def zwaveEvent(physicalgraph.zwave.commands.sensorbinaryv2.SensorBinaryReport cmd, results) {
	if (cmd.sensorType == physicalgraph.zwave.commandclasses.SensorBinaryV2.SENSOR_TYPE_SMOKE) {
		createSmokeEvents(cmd.sensorValue ? "smoke" : "smokeClear", results)
	}
}

def zwaveEvent(physicalgraph.zwave.commands.sensoralarmv1.SensorAlarmReport cmd, results) {
	if (cmd.sensorType == 1) {
		createSmokeEvents(cmd.sensorState ? "smoke" : "smokeClear", results)
	}
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd, results) {
	results << createEvent(descriptionText: "$device.displayName woke up", isStateChange: false)
	if (!state.lastbatt || (now() - state.lastbatt) >= 56*60*60*1000) {
		results << response([
				zwave.batteryV1.batteryGet().format(),
				"delay 2000",
				zwave.wakeUpV1.wakeUpNoMoreInformation().format()
			])
	} else {
		results << response(zwave.wakeUpV1.wakeUpNoMoreInformation())
	}
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd, results) {
	def map = [ name: "battery", unit: "%", isStateChange: true ]
	state.lastbatt = now()
	if (cmd.batteryLevel == 0xFF) {
		map.value = 1
		map.descriptionText = "$device.displayName battery is low!"
	} else {
		map.value = cmd.batteryLevel
	}
	results << createEvent(map)
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd, results) {
	def encapsulatedCommand = cmd.encapsulatedCommand([ 0x80: 1, 0x84: 1, 0x71: 2, 0x72: 1 ])
	state.sec = 1
	log.debug "encapsulated: ${encapsulatedCommand}"
	if (encapsulatedCommand) {
		zwaveEvent(encapsulatedCommand, results)
	} else {
		log.warn "Unable to extract encapsulated cmd from $cmd"
		results << createEvent(descriptionText: cmd.toString())
	}
}

def zwaveEvent(physicalgraph.zwave.Command cmd, results) {
	def event = [ displayed: false ]
	event.linkText = device.label ?: device.name
	event.descriptionText = "$event.linkText: $cmd"
	results << createEvent(event)
}
