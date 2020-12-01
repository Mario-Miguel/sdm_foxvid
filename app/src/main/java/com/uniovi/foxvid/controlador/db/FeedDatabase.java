package com.uniovi.foxvid.controlador.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.uniovi.foxvid.modelo.News;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class FeedDatabase extends SQLiteOpenHelper {

    // Mapeado rápido de indices
    private static final int COLUMN_ID = 0;
    private static final int COLUMN_TITULO = 1;
    private static final int COLUMN_DESC = 2;
    private static final int COLUMN_URL = 3;
    private static final int COLUMN_URL_MINIATURA = 4;

    /*
    Instancia singleton
    */
    private static FeedDatabase singleton;

    /*
    Etiqueta de depuración
     */
    private static final String TAG = FeedDatabase.class.getSimpleName();


    /*
    Nombre de la base de datos
     */
    public static final String DATABASE_NAME = "Feed.db";

    /*
    Versión actual de la base de datos
     */
    public static final int DATABASE_VERSION = 1;


    private FeedDatabase(Context context) {
        super(context,
                DATABASE_NAME,
                null,
                DATABASE_VERSION);

    }

    /**
     * Retorna la instancia unica del singleton
     *
     * @param context contexto donde se ejecutarán las peticiones
     * @return Instancia
     */
    public static synchronized FeedDatabase getInstance(Context context) {
        if (singleton == null) {
            singleton = new FeedDatabase(context.getApplicationContext());
        }
        return singleton;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Crear la tabla 'entrada'
        db.execSQL(ScriptDatabase.CREAR_ENTRADA);


    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Añade los cambios que se realizarán en el esquema
        db.execSQL("DROP TABLE IF EXISTS " + ScriptDatabase.ENTRADA_TABLE_NAME);
        onCreate(db);
    }

    /**
     * Obtiene todos los registros de la tabla entrada
     *
     * @return cursor con los registros
     */
    public Cursor obtenerEntradas() {
        // Seleccionamos todas las filas de la tabla 'entrada'
        return getWritableDatabase().rawQuery(
                "select * from " + ScriptDatabase.ENTRADA_TABLE_NAME, null);
    }

    public List<News> getNews(){
        int id;
        String titulo;
        String descripcion;
        String url;
        String urlImage;
        List<News> salida = new ArrayList<News>();
        Cursor c = obtenerEntradas();
        while (c.moveToNext()){
            id = c.getInt(COLUMN_ID);
            titulo = c.getString(COLUMN_TITULO);
            descripcion = c.getString(COLUMN_DESC);
            url = c.getString(COLUMN_URL);
            urlImage = c.getString(COLUMN_URL_MINIATURA);
            salida.add(new News(titulo,descripcion,url,urlImage));
        }

        return salida;
    }

    /**
     * Inserta un registro en la tabla entrada
     *
     * @param titulo      titulo de la entrada
     * @param descripcion desripcion de la entrada
     * @param url         url del articulo
     * @param url_miniatura   url de la miniatura
     */
    public void insertarEntrada(
            String titulo,
            String descripcion,
            String url,
            String url_miniatura) {

        ContentValues values = new ContentValues();
        values.put(ScriptDatabase.ColumnEntradas.TITULO, titulo);
        values.put(ScriptDatabase.ColumnEntradas.DESCRIPCION, descripcion);
        values.put(ScriptDatabase.ColumnEntradas.URL, url);
        values.put(ScriptDatabase.ColumnEntradas.URL_MINIATURA, url_miniatura);

        // Insertando el registro en la base de datos
        getWritableDatabase().insert(
                ScriptDatabase.ENTRADA_TABLE_NAME,
                null,
                values
        );
    }

    /**
     * Modifica los valores de las columnas de una entrada
     *
     * @param id          identificador de la entrada
     * @param titulo      titulo nuevo de la entrada
     * @param descripcion descripcion nueva para la entrada
     * @param url         url nueva para la entrada
     * @param url_miniatura   url nueva para la miniatura de la entrada
     */
    public void actualizarEntrada(int id,
                                  String titulo,
                                  String descripcion,
                                  String url,
                                  String url_miniatura) {

        ContentValues values = new ContentValues();
        values.put(ScriptDatabase.ColumnEntradas.TITULO, titulo);
        values.put(ScriptDatabase.ColumnEntradas.DESCRIPCION, descripcion);
        values.put(ScriptDatabase.ColumnEntradas.URL, url);
        values.put(ScriptDatabase.ColumnEntradas.URL_MINIATURA, url_miniatura);

        // Modificar entrada
        getWritableDatabase().update(
                ScriptDatabase.ENTRADA_TABLE_NAME,
                values,
                ScriptDatabase.ColumnEntradas.ID + "=?",
                new String[]{String.valueOf(id)});

    }

    /**
     * Procesa una lista de items para su almacenamiento local
     * y sincronización.
     *
     * @param entries lista de items
     */
    public void sincronizarEntradas(List<News> entries) {
    /*
    #1  Mapear temporalemente las entradas nuevas para realizar una
        comparación con las locales
    */
        HashMap<String, News> entryMap = new HashMap<String, News>();
        for (News e : entries) {
            entryMap.put(e.getTitle(), e);
        }

    /*
    #2  Obtener las entradas locales
     */
        Log.i(TAG, "Consultar items actualmente almacenados");
        Cursor c = obtenerEntradas();
        assert c != null;
        Log.i(TAG, "Se encontraron " + c.getCount() + " entradas, computando...");

    /*
    #3  Comenzar a comparar las entradas
     */
        int id;
        String titulo;
        String descripcion;
        String url;
        String urlImage;

        while (c.moveToNext()) {

            id = c.getInt(COLUMN_ID);
            titulo = c.getString(COLUMN_TITULO);
            descripcion = c.getString(COLUMN_DESC);
            url = c.getString(COLUMN_URL);

            News match = entryMap.get(titulo);
            if (match != null) {
                // Filtrar entradas existentes. Remover para prevenir futura inserción
                entryMap.remove(titulo);

            /*
            #3.1 Comprobar que las noticias tienen que ver con el COVID
            */
                if (titulo != null && descripcion != null) {
                    if (!(titulo.contains("covid") || descripcion.contains("covid"))) {
                        entryMap.remove(titulo);
                    }
                }

            }

        }
        c.close();

    /*
    #4 Añadir entradas nuevas
    */
        for (News e : entryMap.values()) {
            Log.i(TAG, "Insertado: titulo=" + e.getTitle());
            insertarEntrada(
                    e.getTitle(),
                    e.getSummary(),
                    e.getUrlNews(),
                    e.getImage()
            );
        }
        Log.i(TAG, "Se actualizaron los registros");


    }

}
