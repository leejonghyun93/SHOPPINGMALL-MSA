/**
 * 운영용 스마트 이미지 시스템 - 실제 이미지 표시
 */
export function useSmartImages() {
    const BASE_IMAGE_PATH = '/images/banners/products/'
    const API_GATEWAY_URL = 'http://13.209.253.241:8080'

    const getProductImage = (product) => {
        console.log('🔍 getProductImage 호출됨:', product)

        // 🔥 DB에서 /upload/product/main/파일명.jpg 패턴 처리
        if (product.mainImage && product.mainImage.startsWith('/upload/product/main/')) {
            const fileName = product.mainImage.split('/').pop()
            const finalUrl = `${API_GATEWAY_URL}/images/${fileName}`
            console.log('✅ DB 업로드 이미지:', finalUrl)
            return finalUrl
        }

        // 🔥 /upload/파일명.jpg 패턴 처리
        if (product.mainImage && product.mainImage.startsWith('/upload/') && !product.mainImage.includes('/product/main/')) {
            const fileName = product.mainImage.split('/').pop()
            const finalUrl = `${API_GATEWAY_URL}/images/${fileName}`
            console.log('✅ 서버 업로드 이미지:', finalUrl)
            return finalUrl
        }

        // 🔥 UUID 파일명만 있는 경우
        if (product.mainImage && !product.mainImage.startsWith('/') && !product.mainImage.startsWith('http')) {
            if (product.mainImage.includes('-') && product.mainImage.length > 30) {
                const finalUrl = `${API_GATEWAY_URL}/images/${product.mainImage}`
                console.log('✅ UUID 파일명 이미지:', finalUrl)
                return finalUrl
            }
        }

        // 🔥 외부 URL (https://images.unsplash.com)
        if (product.mainImage && product.mainImage.startsWith('http')) {
            console.log('✅ 외부 이미지 URL:', product.mainImage)
            return product.mainImage
        }

        // 🔥 프론트엔드 static 이미지 (/images/로 시작)
        if (product.mainImage && product.mainImage.startsWith('/images/')) {
            console.log('✅ 프론트엔드 static 이미지:', product.mainImage)
            return product.mainImage
        }

        // 🔥 image 필드도 확인
        if (product.image) {
            if (product.image.startsWith('/upload/product/main/')) {
                const fileName = product.image.split('/').pop()
                const finalUrl = `${API_GATEWAY_URL}/images/${fileName}`
                console.log('✅ DB 업로드 이미지 (image 필드):', finalUrl)
                return finalUrl
            }

            if (product.image.startsWith('http')) {
                console.log('✅ 외부 이미지 URL (image 필드):', product.image)
                return product.image
            }
        }

        // 🔥 실제 상품 이미지 - 프로젝트 내 파일 사용
        const productImageMap = {
            70: 'oatmeal.jpg',        // 무농약 오트밀
            69: 'strawberry.jpg',     // 국내산 딸기
            68: 'seafood.jpg',        // 모듬 해물탕
            67: 'meal-kit.jpg',       // 부대찌개 밀키트
            66: 'banana.jpg',         // 고당도 바나나
            60: 'apple.jpg',          // 아오리 사과
            62: 'dumpling.jpg',       // 수제 왕만두
            63: 'vitamin.jpg'         // 비타민C
        }

        if (productImageMap[product.id]) {
            const finalUrl = `${BASE_IMAGE_PATH}${productImageMap[product.id]}`
            console.log('✅ 프로젝트 내 실제 이미지:', finalUrl)
            return finalUrl
        }

        // 🔥 기본 이미지
        const defaultImage = `${BASE_IMAGE_PATH}default-product.jpg`
        console.log('⚠️ 기본 이미지 사용:', defaultImage)
        return defaultImage
    }

    const handleImageError = (event) => {
        console.log('❌ 이미지 로드 실패:', event.target.src)

        // 기본 이미지로 교체
        if (!event.target.src.includes('default-product.jpg')) {
            console.log('🔄 기본 이미지로 교체')
            event.target.src = `${BASE_IMAGE_PATH}default-product.jpg`
        } else {
            // 기본 이미지도 실패하면 숨김
            console.log('🚫 이미지 숨김 처리')
            event.target.style.display = 'none'
        }
    }

    const handleImageLoad = (event) => {
        console.log('✅ 이미지 로드 성공:', event.target.src)
        event.target.style.opacity = '1'
    }

    return {
        getProductImage,
        handleImageError,
        handleImageLoad
    }
}