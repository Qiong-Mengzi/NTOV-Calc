package nano.yumeko.ntovCalc

import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.FileConfiguration

class NtovCalc : JavaPlugin() {

    private val calc = Calculator(10)

    override fun onEnable() {
        // Plugin startup logic
        saveDefaultConfig()
        try {
            calc.setTokenLimit(config.getInt("MaxTokenLimit"))
        } catch (e: Exception) {
            logger.warning("Initialization of Calculator failed.")
            throw e
        }
        logger.info("Initialization succeeded.")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (label == "calc") {
            if (args.isNotEmpty()) {
                val expr = args.joinToString(" ") + " -"
                val scanResult = calc.scan(expr)
                if (scanResult.getError() < 0 && scanResult.getError() != -1145) {
                    val analyzeResult = calc.analyze(scanResult)
                    if (analyzeResult.flag == Calculator.AnalyzeFlag.SUCCEED) {
                        sender.sendMessage(calc.calc(analyzeResult.result).toString())
                    } else {
                        sender.sendMessage("Syntax Error: " + analyzeResult.errorReason.op + " ( " + analyzeResult.flag.toString() + " )")
                    }
                } else if (scanResult.getError() == -1145) {
                    sender.sendMessage("Expression too long...")
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
