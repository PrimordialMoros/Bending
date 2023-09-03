CREATE TABLE IF NOT EXISTS bending_users (
    user_id         ${uuidType}           NOT NULL,
    board           BOOLEAN               NOT NULL DEFAULT TRUE,
    CONSTRAINT bending_users_pkey PRIMARY KEY (user_id)
)${extraTableOptions};

-- Ability registry
CREATE TABLE IF NOT EXISTS bending_abilities (
    ability_id      ${uuidType}           NOT NULL,
    ability_name    CHARACTER VARYING(32) NOT NULL,
    CONSTRAINT bending_abilities_uniqueness UNIQUE (ability_name),
    CONSTRAINT bending_abilities_pkey PRIMARY KEY (ability_id)
)${extraTableOptions};

-- Elements
${defineElementEnumType}
CREATE TABLE IF NOT EXISTS bending_user_elements (
    user_id         ${uuidType}           NOT NULL,
    element         ${elementEnumType}    NOT NULL,
    CONSTRAINT bending_user_elements_user_id_fkey FOREIGN KEY (user_id) REFERENCES bending_users (user_id) ON DELETE CASCADE,
    CONSTRAINT bending_user_elements_pkey PRIMARY KEY (user_id, element)
)${extraTableOptions};

-- Slots and presets
CREATE TABLE IF NOT EXISTS bending_presets (
    preset_id       ${uuidType}           NOT NULL,
    user_id         ${uuidType}           NOT NULL,
    preset_name     CHARACTER VARYING(16) NOT NULL,
    CONSTRAINT bending_presets_uniqueness UNIQUE (user_id, preset_name),
    CONSTRAINT bending_presets_user_id_fkey FOREIGN KEY (user_id) REFERENCES bending_users (user_id) ON DELETE CASCADE,
    CONSTRAINT bending_presets_pkey PRIMARY KEY (preset_id)
)${extraTableOptions};

CREATE TABLE IF NOT EXISTS bending_preset_slots (
    preset_id       ${uuidType}           NOT NULL,
    slot            SMALLINT              NOT NULL,
    ability_id      ${uuidType}           NOT NULL,
    CONSTRAINT bending_slot_validity CHECK (slot >= 1 AND slot <= 9),
    CONSTRAINT bending_preset_slots_preset_id_fkey FOREIGN KEY (preset_id) REFERENCES bending_presets (preset_id) ON DELETE CASCADE,
    CONSTRAINT bending_preset_slots_ability_id_fkey FOREIGN KEY (ability_id) REFERENCES bending_abilities (ability_id) ON DELETE CASCADE,
    CONSTRAINT bending_preset_slots_pkey PRIMARY KEY (preset_id, slot)
)${extraTableOptions};

-- Views
CREATE VIEW bending_profiles AS
    SELECT users.user_id, presets.preset_name, slots.slot, slots.ability_id
    FROM bending_users AS users
    INNER JOIN bending_presets AS presets ON users.user_id = presets.user_id
    INNER JOIN bending_preset_slots AS slots ON presets.preset_id = slots.preset_id;

CREATE VIEW bending_profiles_simple AS
    SELECT profiles.user_id, profiles.preset_name, profiles.slot, abilities.ability_name
    FROM bending_profiles AS profiles
    INNER JOIN bending_abilities AS abilities ON profiles.ability_id = abilities.ability_id;
