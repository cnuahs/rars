# Reduced RARS, with support for the base 32-bit integer instruction set (RV32I) ONLY!

To build RARS you will need a [Java SDK](https://www.oracle.com/java/technologies/downloads/).

To build the .jar file, on MacOS or Linux, run the modified build script in the root of the RARS repository:

```bash
$ ./build-jar.sh
```

This compiles the .java source in `src/` placing the resulting .class files in `build/`, strips out any .class files from `build/rars/riscv/instructions/` that are NOT listed in `rv32i.includes`, `extra.includes` or `abstract.includes` (the required abstract base classes), and then builds the .jar file (`rars_rv32i.jar`).

The resulting .jar file should work across all platforms with a Jave runtime environment.

To strip the unwanted instruction .class files we use [rsync](https://rsync.samba.org/) and it's --remove-source-files directive.

## Caveats

Since v1.5, RARS has allowed switching between 32-bit and 64-bit base instruction sets. This reduced version of RARS therefore actually includes support for the 32-bit (RV32I) *and* 64-bit (RV64I) base integer instruction sets. 

This reduced version also retains support for `uret` (return from interrupt/exception; from the U extension) and `wfi` (wait for interrupt) control transfer instructions, and the priviledged control status register (CSR) instructions.

This means you can still switch RARS between 32-bit and 64-bit modes, via the command line or GUI, and in *most* scenarios you should get sane behaviour. BUT, assembling code containing 32-bit or 64-bit mnemonics from the M, F or D extensions will fail.

## Resources

Instructions for the RV32I/RV64I base integer instruction sets are listed in `instr_dict.yaml`.

This dictionary was generated using the parser from RISC-V International (see https://github.com/riscv/riscv-opcodes.git).

Specifically:
```bash
$ git clone git@github.com:riscv/riscv-opcodes.git riscv-opcodes.git
$ cd riscv-opcodes.git
$ ./parse.py rv_i rv32_i rv64_i
```

The list of instruction .class files to "include" (see `rv32i.includes`) was then generated from this dictionary:

```bash
$ cat instr_dict.yaml | grep -e '^[a-z]' | sed -e 's/://' | tr '[:lower:]' '[:upper:]' | xargs -I % echo %.class >rv32i.includes
```

This is quick... not elegant.
