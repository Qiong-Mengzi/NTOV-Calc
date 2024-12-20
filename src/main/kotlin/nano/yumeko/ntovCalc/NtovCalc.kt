package nano.yumeko.ntovCalc

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

import nano.yumeko.ntovCalc.Calculator
import kotlin.math.exp

class NtovCalc : JavaPlugin() {

    val Calc = Calculator()

    override fun onEnable() {
        // Plugin startup logic
        saveResource("config.yml", false)

    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (label == "calc") {
            if (args.isNotEmpty()) {
                val expr = args.joinToString(" ") + " -"
                val scanResult = Calc.scan(expr)
                if (scanResult.getError() < 0) {
                    val analyzeResult = Calc.analyze(scanResult)
                    if (analyzeResult.errorReason.op.isEmpty()) {
                        sender.sendMessage(Calc.calc(analyzeResult.result).toString())
                    } else {
                        sender.sendMessage("Syntax Error: " + analyzeResult.errorReason.op)
                    }
                } else {
                    sender.sendMessage("Unknown Character " + expr[scanResult.getError()] + " at index " + scanResult.getError())
                }
            } else {
                sender.sendMessage("[Usage] /calc <expression>")
            }
            return true
        }
        return false
    }
}
