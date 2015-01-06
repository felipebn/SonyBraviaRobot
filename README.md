SonyBraviaRobot
===============

A robot to power on and off a Sony Bravia TV.
## First use

Run the following command:
```
java -jar sonybraviarobot.jar -ip TV_IP -mac TV_MAC -iniciar
```
When running it for the very first time it will register the executing "device" on the TV asking for the PIN which will show up on TV screen:

```
Digite o PIN exibido na TV:
xxxx[ENTER]
```

## How to use it to power on and open the browser ?

```
java -jar sonybraviarobot.jar -ip TV_IP -mac TV_MAC -iniciar
```

## How to use it to power off the TV ?

```
java -jar sonybraviarobot.jar -ip TV_IP -mac TV_MAC -desligar
```
