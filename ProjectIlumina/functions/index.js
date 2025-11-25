const { onValueCreated, onValueUpdated, onValueDeleted } = require("firebase-functions/v2/database");
const { onRequest } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.database();


function topicFromCidade(cidade) {
  if (!cidade) return null;
  const c = cidade.toLowerCase().trim();

  if (c.includes("torres")) return "torres";
  if (c.includes("capao") || c.includes("capão")) return "capao";

  return null; 
}

async function montarFeedItem(denunciaId, denunciaData) {
  const uid = denunciaData.userId;
  if (!uid) return null;

  const userSnap = await db.ref(`users/${uid}`).get();
  const user = userSnap.val() || {};

  return {
    id: denunciaId,
    denunciaId: denunciaId,
    userId: uid,

    problema: denunciaData.problema || "",
    descricao: denunciaData.descricao || "",
    imagemUrl: denunciaData.imagemUrl || "",

    nomeUsuario: user.nome || "Usuário",
    cidade: user.cidade || denunciaData.cidade || "",
    fotoPerfilUrl: user.imgperfil || "",

    curtidas: denunciaData.curtidas || 0,
    likes: denunciaData.likes || {},

    deletada: false,
    dataHora: denunciaData.dataHora || ""
  };
}
exports.onDenunciaCreated = onValueCreated(
  "/denuncias/{denunciaId}",
  async (event) => {
    const denunciaId = event.params.denunciaId;
    const denuncia = event.data.val();
    if (!denuncia) return;

 
    const feedItem = await montarFeedItem(denunciaId, denuncia);
    if (feedItem) {
      await db.ref(`feed/${denunciaId}`).set(feedItem);
    }


    const cidadeDestino = denuncia.cidade;
    const topic = topicFromCidade(cidadeDestino);
    if (!topic) {
      console.log("Cidade sem tópico mapeado:", cidadeDestino);
      return;
    }

    const payload = {
      data: {
        denunciaId,
        cidade: cidadeDestino,
        problema: denuncia.problema || "",
        descricao: denuncia.descricao || "",
        autorId: denuncia.userId || ""
}

    };

    console.log("Enviando DATA para tópico:", topic, payload.data);

    await admin.messaging().send({
      topic,
      ...payload
    });

    return;
  }
);


exports.onDenunciaUpdated = onValueUpdated(
  "/denuncias/{denunciaId}",
  async (event) => {
    const denunciaId = event.params.denunciaId;
    const before = event.data.before.val();
    const after = event.data.after.val();
    if (!after) return;

    const feedItem = await montarFeedItem(denunciaId, after);
    if (feedItem) {
      await db.ref(`feed/${denunciaId}`).update(feedItem);
    }

    const cidadeAntes = before?.cidade;
    const cidadeDepois = after?.cidade;

    if (cidadeDepois && cidadeDepois !== cidadeAntes) {
      const topic = topicFromCidade(cidadeDepois);
      if (!topic) {
        console.log("Cidade nova sem tópico mapeado:", cidadeDepois);
        return;
      }

      const payload = {
        data: {
          denunciaId: denunciaId,
          cidade: cidadeDepois || "",
          problema: after.problema || "",
          descricao: after.descricao || ""
        }
      };

      console.log("Enviando atualização DATA para tópico:", topic, payload.data);

      await admin.messaging().send({
        topic,
        ...payload
      });
    }

    return;
  }
);


exports.onDenunciaDeleted = onValueDeleted(
  "/denuncias/{denunciaId}",
  async (event) => {
    const denunciaId = event.params.denunciaId;
    await db.ref(`feed/${denunciaId}`).update({ deletada: true });
    return;
  }
);


exports.testSendToToken = onRequest(async (req, res) => {
  const token = req.query.token;
  if (!token) {
    res.status(400).send("Passe ?token=SEU_TOKEN");
    return;
  }

  const payload = {
    data: {
      problema: "Teste de problema",
      descricao: "Se isso chegou, sua Function DATA está OK ✅",
      cidade: "torres",
      denunciaId: "teste123"
    }
  };

  try {
    const r = await admin.messaging().send({
      token,
      ...payload
    });
    res.send("Enviado! id=" + r);
  } catch (e) {
    console.error(e);
    res.status(500).send(e.message);
  }
});
