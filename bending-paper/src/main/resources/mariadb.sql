-- Bending MariaDB Schema

CREATE TABLE IF NOT EXISTS bending_players (
    player_id       INT AUTO_INCREMENT      NOT NULL,
    uuid            BINARY(16)              NOT NULL,
    board           BOOL                    NOT NULL DEFAULT TRUE,
    PRIMARY KEY (player_id)
) DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS bending_abilities (
    ability_id      INT AUTO_INCREMENT      NOT NULL,
    ability_name    VARCHAR(32)                      NOT NULL UNIQUE,
    PRIMARY KEY (ability_id)
) DEFAULT CHARSET = utf8mb4;
CREATE INDEX IF NOT EXISTS ability_name_index ON bending_abilities (ability_name);

CREATE TABLE IF NOT EXISTS bending_elements (
    element_id      INT AUTO_INCREMENT      NOT NULL,
    element_name    VARCHAR(16)              NOT NULL UNIQUE,
    PRIMARY KEY (element_id)
) DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS bending_players_elements (
    element_id      INT                 NOT NULL,
    player_id       INT                 NOT NULL,
    FOREIGN KEY(element_id) REFERENCES bending_elements(element_id) ON DELETE CASCADE,
    FOREIGN KEY(player_id) REFERENCES bending_players(player_id) ON DELETE CASCADE,
    PRIMARY KEY(element_id, player_id)
) DEFAULT CHARSET = utf8mb4;
CREATE INDEX IF NOT EXISTS elements_index ON bending_players_elements (player_id);

CREATE TABLE IF NOT EXISTS bending_players_slots (
    player_id       INT                 NOT NULL,
    slot            SMALLINT            NOT NULL,
    ability_id      INT                 NOT NULL,
    FOREIGN KEY(player_id) REFERENCES bending_players(player_id) ON DELETE CASCADE,
    FOREIGN KEY(ability_id) REFERENCES bending_abilities(ability_id) ON DELETE CASCADE,
    PRIMARY KEY(player_id, slot)
) DEFAULT CHARSET = utf8mb4;
CREATE INDEX IF NOT EXISTS players_slots_index ON bending_players_slots (player_id);

CREATE TABLE IF NOT EXISTS bending_presets (
    preset_id       INT AUTO_INCREMENT      NOT NULL,
    player_id       INT                     NOT NULL,
    preset_name     VARCHAR(16)             NOT NULL,
    FOREIGN KEY(player_id) REFERENCES bending_players(player_id) ON DELETE CASCADE,
    PRIMARY KEY(preset_id)
) DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS bending_presets_slots (
    preset_id       INT                 NOT NULL,
    slot            SMALLINT            NOT NULL,
    ability_id      INT                 NOT NULL,
    FOREIGN KEY(preset_id) REFERENCES bending_presets(preset_id) ON DELETE CASCADE,
    FOREIGN KEY(ability_id) REFERENCES bending_abilities(ability_id) ON DELETE CASCADE,
    PRIMARY KEY(preset_id, slot)
) DEFAULT CHARSET = utf8mb4;
CREATE INDEX IF NOT EXISTS presets_slots_index ON bending_presets_slots (preset_id);
