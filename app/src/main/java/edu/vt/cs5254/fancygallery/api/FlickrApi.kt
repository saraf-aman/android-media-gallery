package edu.vt.cs5254.fancygallery.api

import retrofit2.http.GET

private const val API_KEY = "5f2cbd240a4c5d529dc450f063435fc3"

interface FlickrApi {
    @GET(
        "services/rest/?method=flickr.interestingness.getList" +
                "&api_key=$API_KEY" +
                "&format=json" +
                "&nojsoncallback=1" +
                "&extras=url_s,geo" +
                "&per_page=99"
    )
//    Call<ResponseBody> fetchPhotos(@Query("per_page") Int perPage)
    suspend fun fetchPhotos(): FlickrResponse
}