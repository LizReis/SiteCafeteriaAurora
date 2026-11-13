package br.com.cafeaurora.service;

import br.com.cafeaurora.exception.EntradaInvalidaException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.*;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.*;
import java.awt.image.BufferedImage;
import java.util.Locale;

@Service
public class FotoAvaliacaoService {
    public static final long LIMITE=5L*1024*1024;
    public record Foto(String tipoMime,byte[] conteudo) {}
    public Foto validar(MultipartFile arquivo) {
        if(arquivo==null)return null;
        if(arquivo.getSize()>LIMITE)throw new org.springframework.web.multipart.MaxUploadSizeExceededException(LIMITE);
        if(arquivo.isEmpty())throw invalida();
        try(var entrada=arquivo.getInputStream()) {
            byte[] bytes=entrada.readNBytes((int)LIMITE+1);
            if(bytes.length>LIMITE)throw new org.springframework.web.multipart.MaxUploadSizeExceededException(LIMITE);
            try(var imagem=new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var leitores=ImageIO.getImageReaders(imagem);
            if(!leitores.hasNext())throw invalida();
            ImageReader leitor=leitores.next();
            try {
                String formato=leitor.getFormatName().toLowerCase(Locale.ROOT);
                String mime=switch(formato){case "jpeg","jpg"->"image/jpeg";case "png"->"image/png";case "webp"->"image/webp";default->throw invalida();};
                if(!mime.equals(arquivo.getContentType()))throw invalida();
                leitor.setInput(imagem,true,true);
                int largura=leitor.getWidth(0),altura=leitor.getHeight(0);
                if(largura<1||altura<1||largura>4096||altura>4096||(long)largura*altura>4_000_000)throw invalida();
                boolean[] aviso={false};leitor.addIIOReadWarningListener((r,w)->aviso[0]=true);
                BufferedImage original=leitor.read(0);
                if(original==null||aviso[0])throw invalida();
                // Uma nova imagem e escrita sem metadata descartam EXIF, nomes e perfis de entrada.
                BufferedImage limpa=new BufferedImage(largura,altura,BufferedImage.TYPE_INT_RGB);
                var g=limpa.createGraphics();try {g.setColor(java.awt.Color.WHITE);g.fillRect(0,0,largura,altura);g.drawImage(original,0,0,null);}finally{g.dispose();original.flush();}
                try(var saida=new ByteArrayOutputStream()) {
                    if(!ImageIO.write(limpa,"jpeg",saida))throw invalida();
                    if(saida.size()>LIMITE)throw invalida();
                    return new Foto("image/jpeg",saida.toByteArray());
                } finally {limpa.flush();}
            } finally {leitor.dispose();}
            }
        } catch(org.springframework.web.multipart.MaxUploadSizeExceededException ex){throw ex;}
        catch(IOException|RuntimeException ex){throw invalida();}
    }
    private EntradaInvalidaException invalida(){return new EntradaInvalidaException("Foto inválida. Use JPEG, PNG ou WebP íntegro, até 5 MB e 4 milhões de pixels (máximo 4096 por lado).");}
}
