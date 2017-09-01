#!/bin/bash

java Z2Receiver 6002 6003 &
java Z2Forwarder 6001 6002 &
java Z2Forwarder 6003 6000 &
java Z2Sender 6000 6001 < plik.txt
