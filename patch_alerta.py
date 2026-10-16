#!/usr/bin/env python
import os

BASE = r"C:\Users\ronal\AndroidStudioProjects\MyApplication4\app\src\main\java\com\example\myapplication"
fp = os.path.join(BASE, "fragments/EmotionalReportFragment.java")
with open(fp, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Insertar mostrarNotificacionLocal después de crearAlertaEmocional(bajos);
old1 = """if (bajos >= 3) {
            crearAlertaEmocional(bajos);
        }"""

new1 = """if (bajos >= 3) {
            crearAlertaEmocional(bajos);
            mostrarNotificacionLocal(bajos);
        }"""

content = content.replace(old1, new1)

# 2. Agregar método mostrarNotificacionLocal antes del cierre de la clase
# El cierre es "    }" seguido de "}" de la clase. Buscamos el patrón final.
old2 = """    });


}"""

# Insertar el nuevo método después de onHistorialAlerta y antes del cierre de la clase.
# El método onHistorialAlerta termina en la línea que tiene "    }" (cierre del if), luego viene "}" de la clase.
# Insertaremos el nuevo método entre el cierre de onHistorialAlerta y el cierre de la clase.

new_method = """
    private void mostrarNotificacionLocal(int bajos) {
        String uid = sessionManager.getUserId();
        if (uid == null) return;
        NotificationManager notificationManager =
                (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            String channelId = FcmNotificationSender.CHANNEL_ID;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        channelId,
                        "Alertas Bienestar UPC",
                        NotificationManager.IMPORTANCE_HIGH
                );
                notificationManager.createNotificationChannel(channel);
            }
            Notification.Builder builder = new Notification.Builder(requireContext())
                    .setSmallIcon(R.drawable.ic_notifications)
                    .setContentTitle("Bienestar Universitario")
                    .setContentText("Lleva " + bajos + " días con ánimos bajos. Te acompañaremos.")
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setAutoCancel(true);
            notificationManager.notify(1001, builder.build());
        }
    }"""

# Reemplazamos el cierre actual con el nuevo método + cierre
# El patrón exacto al final del archivo es "    }\\n}" (indent 4 spaces + class close)
# Vamos a reemplazar "    }\\n}" con el método + "    }\\n}"

# Pero hay que tener cuidado: there might be trailing spaces. Buscamos "    }" followed by "}" on next line.
# El archivo termina en "    }\\n}" basado en el read anterior (línea 405-406).

# Sustituiremos "    }\\n}" por el método + "    }\\n}"
# Pero primero confirmemos el contenido exacto. El read muestra:
# 404:                 });
405: }
406: }
El último "}" es el de la clase. El de la línea 404 cierra el método onHistorialAlerta.
El de la línea 405 cierra... revisando: línea 404 cierra el addOnSuccessListener de crearAlertaEmocional.
La línea 405 "}" cierra el método onHistorialAlerta.
La línea 406 "}" cierra la clase.

Entonces entre la línea 404 "});" y la línea 406 "}" final hay la línea 405 "}" que cierra onHistorialAlerta.
Así que para insertar el nuevo método, voy a reemplazar el "}" de la línea 405 (que cierra onHistorialAlerta) por el nuevo método + ese "}" final.

# El patrón a reemplazar es la línea exacta "}" (sola) que cierra onHistorialAlerta, y poner en su lugar el método seguido del cierre.

# Vamos a hacer: reemplazar "    }\\n}" (4 spaces + } + newline + }) con el método completo + "    }\\n}".
# Pero el read muestra líneas sueltas. Voy a intentar reemplazar el bloque final.

# Buscamos el texto exacto que está antes del cierre de la clase.
# Según el read, las últimas líneas son:
# ...
#                 });
#     }
# }
# Entonces el "    }" cierra onHistorialAlerta y el último "}" cierra la clase.

# Voy a reemplazar el "    }" (con 4 espacios) por el método + "    }".
# Pero hay que asegurar que no haya otros "    }". Usaremos contexto.

# Simpler: insertar el método después de la línea "    }" que cierra onHistorialAlerta, y antes de "}" de la clase.
# Usaremos que la línea "    }" aparece justo antes del cierre final.

# Patrón: "    }\\n" followed por "}" al final.
# Voy a reemplazar el "    }" final (el que está antes del último "}") con el método + "    }".

# Reemplazaremos: "    }\\n}" por el método + "    }\\n}".
# Pero hay que contar bien. El read mostró:
# 404:                 });
# 405: }
# 406: }
# Entonces la línea 405 es "}" sola (aunque el read la muestra como "405: }"). Entonces el patrón "    }" con 4 espacios NO está ahí, es solo "}" sin indent.
# Revisando el read: "    }" con sangría aparece en otras partes. En la línea 405 es solo "}" sin espacios.

# Voy a buscar el último "}" antes del de la clase. Vamos a reemplazar el patrón al final del archivo.

# En lugar de jugar con patrones complejos, voy a simplemente insertar el nuevo método buscando la posición después de "crearAlertaEmocional(bajos);\n".
# Ya lo hice en el paso 1. Ahora para el método, voy a buscar "    }" que está después de onHistorialAlerta y reemplazarlo.

# Vamos a hacer algo más simple: usar replace sobre un marcador único.
# Insertaremos el método justo después de "    }" que cierra onHistorialAlerta. 
# Pero para no complicarnos, voy a insertar el método al final del archivo, antes del último "}".

# El archivo termina con:
# ...
#                 });
# }
# }
# El último "}" cierra la clase. El anterior "}" cierra onHistorialAlerta.

# Voy a reemplazar el penúltimo "}" (el que cierra onHistorialAlerta) por el método + el último "}".
# Pero el archivo tiene esos dos "}" seguidos. Voy a reemplazar la ocurrencia de "\n}\n}" (newline + } + newline + }) por "\n" + método + "\n}\n}".

old2 = "\n}\n}"
new2 = "\n" + """
    private void mostrarNotificacionLocal(int bajos) {
        String uid = sessionManager.getUserId();
        if (uid == null) return;
        NotificationManager notificationManager =
                (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            String channelId = FcmNotificationSender.CHANNEL_ID;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        channelId,
                        "Alertas Bienestar UPC",
                        NotificationManager.IMPORTANCE_HIGH
                );
                notificationManager.createNotificationChannel(channel);
            }
            Notification.Builder builder = new Notification.Builder(requireContext())
                    .setSmallIcon(R.drawable.ic_notifications)
                    .setContentTitle("Bienestar Universitario")
                    .setContentText("Lleva " + bajos + " días con ánimos bajos. Te acompañaremos.")
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setAutoCancel(true);
            notificationManager.notify(1001, builder.build());
        }
    }
"""
"""

content = content.replace(old2, new2)

with open(fp, "w", encoding="utf-8") as f:
    f.write(content)

print("EmotionalReportFragment.java actualizado")