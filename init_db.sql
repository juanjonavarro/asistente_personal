CREATE TABLE IF NOT EXISTS asistente (
	id bigserial NOT NULL,
	nombre varchar(200) NULL,
	rol text NULL,
	imagen varchar(200) NULL,
	CONSTRAINT asistente_unique UNIQUE (id)
);

CREATE TABLE IF NOT EXISTS frase_diaria (
	id bigserial NOT NULL,
	texto text NULL,
	autor varchar(200) NULL,
	CONSTRAINT frase_diaria_unique UNIQUE (id)
);

CREATE TABLE IF NOT EXISTS hecho (
	id bigserial NOT NULL,
	clientid varchar(100) NULL,
	fecha timestamp NULL,
	texto text NULL,
	ts timestamp NULL,
	origen varchar(20) NULL,
	CONSTRAINT hecho_unique UNIQUE (id)
);
CREATE INDEX IF NOT EXISTS hecho_clientid_idx ON hecho USING btree (clientid);
CREATE INDEX IF NOT EXISTS hecho_ts_idx ON hecho USING btree (ts);

CREATE TABLE IF NOT EXISTS mensaje_chat (
	clientid varchar(100) NOT NULL,
	sender varchar(20) NOT NULL,
	message text NOT NULL,
	ts timestamp NOT NULL,
	id bigserial NOT NULL,
	CONSTRAINT mensaje_chat_unique UNIQUE (id)
);

CREATE INDEX IF NOT EXISTS mensaje_chat_clientid_idx ON mensaje_chat USING btree (clientid);
CREATE INDEX IF NOT EXISTS mensaje_chat_ts_idx ON mensaje_chat USING btree (ts);

CREATE TABLE IF NOT EXISTS sistema (
	asistente_activo int8 NULL,
	frase_diaria int8 NULL
);

CREATE TABLE IF NOT EXISTS usuario (
	clientid varchar(100) NOT NULL,
	nombre varchar(200) NOT NULL,
	estado varchar(20) NOT NULL,
	id_ultimo_saludo int8 NULL,
	asistente bigint DEFAULT 1 NULL,
	CONSTRAINT usuario_unique UNIQUE (clientid)
);

INSERT INTO asistente
(nombre, rol, imagen)
VALUES('El Fary', 'Eres Fary, el famoso cantante español. Utiliza sus giros y su filosofía ante la vida', 'elfary.png');
INSERT INTO asistente
(nombre, rol, imagen)
VALUES('El Monje Budista', 'Eres un monje budista frustrado por su celibato. Cree en la naturaleza y la espiritualidad pero los placeres del cuerpo te atan a una vida llena de culpa y remordimiento', 'monjebudista.png');
INSERT INTO asistente
(nombre, rol, imagen)
VALUES('Iker Jimenez', 'Eres el divulgador Iker Jimenez.', 'iker.jpg');
INSERT INTO asistente
(nombre, rol, imagen)
VALUES('Darth Vader', 'Eres Darth Vader', 'darthvader.jpg');

INSERT INTO frase_diaria
(texto, autor)
VALUES('Se necesitan dos años para aprender a hablar y sesenta para aprender a callar', 'Ernest Hemingway');
INSERT INTO frase_diaria
(texto, autor)
VALUES('Todas las palabras fueron alguna vez un neologismo', 'Jorge Luis Borges');
INSERT INTO frase_diaria
(texto, autor)
VALUES('Dime y lo olvido, enséñame y lo recuerdo, involúcrame y lo aprendo', 'Benjamin Franklin');

INSERT INTO sistema
(asistente_activo, frase_diaria)
VALUES(1, 1);