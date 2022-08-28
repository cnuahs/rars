#!/bin/bash
if git submodule status | grep \( > /dev/null ; then 
    mkdir -p build
    find src -name "*.java" | xargs javac -d build
    if [[ "$OSTYPE" == "darwin"* ]]; then
        find src -type f -not -name "*.java" -exec rsync -R {} build \;
    else
        find src -type f -not -name "*.java" -exec cp --parents {} build \;
    fi
    cp -rf build/src/* build
    rm -r build/src
    cp README.md License.txt build
    cd build

    # keep ONLY those instructions in the RV32I base 32-bit integer instruction set
    rsync --remove-source-files \
          --exclude-from=../rv32i/abstract.includes --exclude-from=../rv32i/rv32i.includes --exclude-from=../rv32i/extra.includes \
          rars/riscv/instructions/* ./jnk/ && rm -rf jnk

    jar cfm ../rars_rv32i.jar ./META-INF/MANIFEST.MF *
else
    echo "It looks like JSoftFloat is not cloned. Consider running \"git submodule update --init\""
fi
