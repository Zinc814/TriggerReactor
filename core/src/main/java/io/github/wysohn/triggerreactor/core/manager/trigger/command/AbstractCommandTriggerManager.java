/*******************************************************************************
 *     Copyright (C) 2018 wysohn
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package io.github.wysohn.triggerreactor.core.manager.trigger.command;

import io.github.wysohn.triggerreactor.core.bridge.ICommandSender;
import io.github.wysohn.triggerreactor.core.main.TriggerReactorCore;
import io.github.wysohn.triggerreactor.core.manager.trigger.AbstractTriggerManager;
import io.github.wysohn.triggerreactor.tools.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public abstract class AbstractCommandTriggerManager extends AbstractTriggerManager<CommandTrigger> {
    protected final Map<String, CommandTrigger> aliasesMap = new CommandMap();

    @Override
    public void reload() {
        triggers.clear();
        aliasesMap.clear();

        for (File file : folder.listFiles()) {
            if (!isTriggerFile(file))
                continue;

            String triggerName = extractName(file);

            File triggerConfigFile = new File(folder, triggerName + ".yml");

            Boolean sync = Boolean.FALSE;
            List<String> permissions = new ArrayList<>();
            List<String> aliases = new ArrayList<>();
            if (triggerConfigFile.isFile() && triggerConfigFile.exists()) {
                try {
                    sync = getData(triggerConfigFile, "sync", Boolean.FALSE);
                    permissions = getData(triggerConfigFile, "permissions", new ArrayList<>());
                    aliases = getData(triggerConfigFile, "aliases", new ArrayList<>());
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
            }

            String script = null;
            try {
                script = FileUtil.readFromFile(file);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }

            CommandTrigger trigger = null;
            try {
                trigger = new CommandTrigger(triggerName, file, script);
            } catch (TriggerInitFailedException e) {
                e.printStackTrace();
                continue;
            }

            trigger.setSync(sync);
            trigger.setPermissions(permissions.toArray(new String[0]));
            trigger.setAliases(aliases.toArray(new String[0]));

            triggers.put(triggerName, trigger);
            registerAliases(trigger);
        }
    }

    @Override
    public void saveAll() {
        Set<String> failed = new HashSet<>();
        for (Entry<String, CommandTrigger> entry : triggers.entrySet()) {
            String triggerName = entry.getKey();
            CommandTrigger trigger = entry.getValue();

            String script = trigger.getScript();

            File file = getTriggerFile(folder, triggerName, true);
            try {
                FileUtil.writeToFile(file, script);
            } catch (Exception e) {
                e.printStackTrace();
                plugin.getLogger().severe("Could not save command trigger for " + triggerName);
                failed.add(triggerName);
            }

            File triggerConfigFile = new File(folder, triggerName + ".yml");
            try {
                triggerConfigFile.createNewFile();
                setData(triggerConfigFile, "sync", trigger.isSync());
                setData(triggerConfigFile, "permissions", trigger.permissions);
                setData(triggerConfigFile, "aliases", trigger.aliases);
            } catch (Exception e) {
                e.printStackTrace();
                plugin.getLogger().severe("Could not save command trigger for " + triggerName);
                failed.add(triggerName);
            }
        }

        for (String key : failed) {
            triggers.remove(key);
        }
    }

    @Override
    protected void deleteInfo(CommandTrigger trigger) {
        if (trigger instanceof CommandTrigger)
            removeAliases(trigger);

        FileUtil.delete(new File(trigger.getFile().getParent(), trigger.getTriggerName() + ".yml"));
        super.deleteInfo(trigger);
    }

    public boolean hasCommandTrigger(String cmd) {
        return triggers.containsKey(cmd);
    }

    public CommandTrigger getCommandTrigger(String cmd) {
        return triggers.get(cmd);
    }

    /**
     * @param adding CommandSender to send error message on script error
     * @param cmd    command to intercept
     * @param script script to be executed
     * @return true on success; false if cmd already binded.
     */
    public boolean addCommandTrigger(ICommandSender adding, String cmd, String script) {
        if (triggers.containsKey(cmd))
            return false;

        File triggerFile = getTriggerFile(folder, cmd, true);
        CommandTrigger trigger = null;
        try {
            trigger = new CommandTrigger(cmd, triggerFile, script);
        } catch (TriggerInitFailedException e1) {
            plugin.handleException(adding, e1);
            return false;
        }

        triggers.put(cmd, trigger);

        plugin.saveAsynchronously(this);
        return true;
    }

    /**
     * @param cmd command to stop intercept
     * @return true on success; false if cmd does not exist.
     */
    public boolean removeCommandTrigger(String cmd) {
        if (!triggers.containsKey(cmd))
            return false;

        deleteInfo(triggers.remove(cmd));

        return true;
    }

    public CommandTrigger createTempCommandTrigger(String script) throws TriggerInitFailedException {
        return new CommandTrigger("temp", null, script);
    }

    public void removeAliases(CommandTrigger trigger) {
        for (String alias : trigger.getAliases()) {
            aliasesMap.remove(alias);
        }
    }

    public void registerAliases(CommandTrigger trigger) {
        for (String alias : trigger.getAliases()) {
            CommandTrigger prev = aliasesMap.get(alias);
            if (prev != null) {
                plugin.getLogger().warning("CommandTrigger " + trigger.getTriggerName() + "'s alias "
                        + alias + " couldn't be registered.");
                plugin.getLogger().warning(alias + " is already used by " + prev.getTriggerName() + ".");
                continue;
            }

            aliasesMap.put(alias, trigger);
        }
    }

    public AbstractCommandTriggerManager(TriggerReactorCore plugin, File tirggerFolder) {
        super(plugin, tirggerFolder);
    }

    private static class CommandMap extends HashMap<String, CommandTrigger> {
        @Override
        public CommandTrigger get(Object o) {
            if (o instanceof String)
                return super.get(((String) o).toLowerCase());
            else
                return super.get(o);
        }

        @Override
        public CommandTrigger put(String s, CommandTrigger commandTrigger) {
            return super.put(s.toLowerCase(), commandTrigger);
        }

        @Override
        public boolean containsKey(Object o) {
            if (o instanceof String)
                return super.containsKey(((String) o).toLowerCase());
            else
                return super.containsKey(o);
        }

        @Override
        public CommandTrigger remove(Object o) {
            if (o instanceof String)
                return super.remove(((String) o).toLowerCase());
            else
                return super.remove(o);
        }
    }
}