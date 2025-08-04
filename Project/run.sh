#!/bin/bash
# Convert input to lowercase
input=$(echo "${2:-client}" | tr '[:upper:]' '[:lower:]')
port=${3:-3000}  # Default port to 3000 if not provided

# The first argument ($1) is the classpath (e.g., 'bin' or 'src')
classpath_arg="$1"

# Default debug mode to false
debug=false
debugArg=""

# Check for -d flag
if [[ " $@ " =~ " -d " ]]; then
    debug=true
fi

if $debug; then
    # Used for binding to vs code debug mode
    debugArg="-agentlib:jdwp=transport=dt_socket,server=y,address=5005"
    echo "Debug mode is ON"
fi

if [ "$input" = "server" ]; then
    # Corrected command: -cp sets the classpath, then provide the FQCN
    java $debugArg -cp "$classpath_arg" server.Server "$port"
elif [ "$input" = "client" ]; then
    # Corrected command: -cp sets the classpath, then provide the FQCN
    java $debugArg -cp "$classpath_arg" client.Client
    # In Milestone3 changes Client to ClientUI
elif [ "$input" = "ui" ]; then
    # Corrected command: -cp sets the classpath, then provide the FQCN
    java $debugArg -cp "$classpath_arg" client.ClientUI
    # Milestone 3's new entry point
else
    echo "Must specify client or server for MS2 or ui or server for MS3"
fi