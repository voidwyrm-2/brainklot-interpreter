//package org.NuclearPasta.brainklot.run
//import org.NuclearPasta.brainklot.interpreter.*

/*
private fun runinit() {
    println()
}
*/

// run +++>+++++<[->+<]>.

fun main() {
    //runinit()
    while (true) {
        print(">>> ")
        val inp: String = readlnOrNull() ?: continue
        if (inp == "exit" || inp == "quit") return
        else {
            runInterpreter(inp)
        }/* else if (inp.startsWith("file ") && inp.length > 5) {

        }*/
    }
}